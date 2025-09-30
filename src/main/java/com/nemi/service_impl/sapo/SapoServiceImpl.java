package com.nemi.service_impl.sapo;

import com.nemi.client.SapoClient;
import com.nemi.constant.enums.PosName;
import com.nemi.constant.enums.PosStatus;
import com.nemi.entity.PosEntity;
import com.nemi.entity.ProductEntity;
import com.nemi.entity.ProductVariantEntity;
import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.request.sapo.SapoRequest;
import com.nemi.model.response.PosConnectionResponse;
import com.nemi.model.response.sapo.SapoAccessTokenResponse;
import com.nemi.model.response.sapo.SapoProductResponse;
import com.nemi.repository.PosRepository;
import com.nemi.repository.ProductRepository;
import com.nemi.repository.ProductVariantRepository;
import com.nemi.service.PosManagementService;
import com.nemi.util.ClaimUtil;
import com.nemi.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SapoServiceImpl implements PosManagementService {

    private final ClaimUtil claimUtil;
    private final SapoClient sapoClient;
    private final ProductRepository productRepository;
    private final PosRepository posRepository;
    private final ProductVariantRepository productVariantRepository;


    @Override
    public String getPosName() {
        return PosName.SAPO.getValue();
    }

    @Override
    public PosConnectionResponse connectPos(PosConnectionRequest posConnectionRequest) {
        try {
            String userId = claimUtil.getUserId();

            Map<String, String> configMap = new HashMap<>();
            configMap.put("clientId", posConnectionRequest.getClientId());
            configMap.put("clientSecret", posConnectionRequest.getClientSecret());
            configMap.put("storeName", posConnectionRequest.getStoreName());

            SapoAccessTokenResponse tokenResponse = sapoClient.getAccessToken(posConnectionRequest);
            if (tokenResponse.getAccessToken() == null) {
                log.error("Sapo response does not contain accessToken: {}", tokenResponse);
            }

            PosEntity posEntityBuilder = PosEntity.builder()
                    .posName(PosName.SAPO.getValue())
                    .userId(userId)
                    .accessToken(tokenResponse.getAccessToken())
                    .status(PosStatus.ACTIVE.name())
                    .config(JsonUtils.toJson(configMap))
                    .companyId(String.valueOf(claimUtil.getCompanyId()))
                    .createdBy(claimUtil.getUserName())
                    .build();

            posRepository.save(posEntityBuilder);
            PosConnectionResponse posConnectionResponse = PosConnectionResponse.toPosConnectionResponse(posEntityBuilder);
            log.info("Sapo response is {}", posConnectionResponse);
            return posConnectionResponse;
        } catch (Exception e) {
            log.error("Exchange token failed: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_FAILED));
        }
    }

    @Override
    public boolean syncData(String posId) {
        // sync product from Sapo

        //B1 : Lay posentity va validate posName
        PosEntity posEntity = getPos(posId);

        //B2 : parse Config (khuyen khich dung JsonUtils)
        @SuppressWarnings("unchecked")
        Map<String, String> configMap = JsonUtils.fromJson(posEntity.getConfig(), Map.class);

        String clientId = configMap.get("clientId");
        String clientSecret = configMap.get("clientSecret");
        String storeName = configMap.get("storeName");
        String accessToken = posEntity.getAccessToken();

        List<ProductEntity> allProducts = new ArrayList<>();
        List<ProductVariantEntity> allVariants = new ArrayList<>();

        //chi set size cho lan dau tien
        Map<String, Object> paginator = new HashMap<>();
        paginator.put("limit", 50);

        SapoRequest request = SapoRequest.builder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .storeName(storeName)
                .accessToken(accessToken)
                .paginator(paginator)
                .build();
        //B3 : goi SapoClient de lay du lieu
        while (true) {
            Optional<SapoProductResponse> responseOpt = sapoClient.getProducts(request);

            if(responseOpt.isEmpty()) {
                log.error("[SapoServiceImpl.syncData] response is empty");
                return false;
            }

            SapoProductResponse response = responseOpt.get();
            if(response.getProducts() == null || response.getProducts().isEmpty()) {
                log.info("[SapoServiceImpl.syncData] No more products to sync");
                break;
            }

            // Convert products and variants
            for (SapoProductResponse.Product sapoProduct : response.getProducts()) {
                ProductEntity productEntity = convertToProductEntity(posId, sapoProduct);
                allProducts.add(productEntity);

                // Convert variants
                if (sapoProduct.getVariants() != null && !sapoProduct.getVariants().isEmpty()) {
                    List<ProductVariantEntity> variants = convertToVariantEntities(productEntity.getProductId(), sapoProduct.getVariants());
                    allVariants.addAll(variants);
                }
            }

            log.info("Fetched {} products and {} variants", response.getProducts().size(), allVariants.size());

            // Sapo API uses limit-based pagination, so we break after first call
            // In real implementation, you might need to handle pagination differently
            break;
        }

        saveAllProductsSync(allProducts);
        saveAllVariantsSync(allVariants);

        log.info("Successfully synced {} products and {} variants from Sapo", allProducts.size(), allVariants.size());
        return true;
    }

    @Override
    public boolean syncOrder(String posId) {
        return false;
    }

    public PosEntity getPos(String posId) {
        log.debug("[SapoSyncDataImpl.getPos] posId: {}", posId);

        // Lấy PosEntity từ DB
        return posRepository.findById(posId)
                .orElseThrow(() -> {
                    log.error("Error [SapoSyncDataImpl.getPos] not found posId: {}", posId);
                    return new TechnicalException(AlertMessages.alert(TechnicalAlertCode.DATA_INVALID));
                });
    }


    private ProductEntity convertToProductEntity(String posId, SapoProductResponse.Product apiProduct) {
        ProductEntity product = new ProductEntity();

        product.setPosId(posId);
        product.setProductId(String.valueOf(apiProduct.getId()));
        // Set code from first variant's SKU
        product.setCode(null);
        product.setName(apiProduct.getName());
        product.setDescription(apiProduct.getContent());
        product.setBrand(apiProduct.getVendor());
        product.setCategory(apiProduct.getProductType());
        product.setStatus(apiProduct.getStatus());
        product.setImages(JsonUtils.toJson(apiProduct.getImages().stream()
                .map(SapoProductResponse.Image::getSrc) // Dùng method reference
                .collect(Collectors.toList())));

        // Set timestamps - parse from string format
        if (apiProduct.getCreatedOn() != null && !apiProduct.getCreatedOn().isEmpty()) {
            product.setCreatedDatetime(parseSapoDateTime(apiProduct.getCreatedOn()));
        }
        if (apiProduct.getModifiedOn() != null && !apiProduct.getModifiedOn().isEmpty()) {
            product.setUpdatedDatetime(parseSapoDateTime(apiProduct.getModifiedOn()));
        }
        return product;
    }

    private List<ProductVariantEntity> convertToVariantEntities(String productId, List<SapoProductResponse.Variant> apiVariants) {
        return apiVariants.stream()
                .map(apiVariant -> convertToVariantEntity(productId, apiVariant))
                .collect(Collectors.toList());
    }

    private ProductVariantEntity convertToVariantEntity(String productId, SapoProductResponse.Variant apiVariant) {
        ProductVariantEntity variant = new ProductVariantEntity();

        // Required fields
        variant.setVariantId(String.valueOf(apiVariant.getId()));
        variant.setProductId(productId);

        // Handle nullable fields with defaults
        variant.setSku(apiVariant.getSku() != null ? apiVariant.getSku() : "");
        variant.setBarcode(apiVariant.getBarcode() != null ? apiVariant.getBarcode() : "");

        // Parse price
        if (apiVariant.getPrice() != null) {
            try {
                variant.setPrice(BigDecimal.valueOf(apiVariant.getPrice()));
            } catch (NumberFormatException e) {
                log.warn("Failed to parse price: {}", apiVariant.getPrice());
                variant.setPrice(BigDecimal.ZERO);
            }
        } else {
            variant.setPrice(BigDecimal.ZERO);
        }

        variant.setCcy("VND"); // Default currency
        variant.setInventoryQuantity(apiVariant.getInventoryQuantity() != null ? apiVariant.getInventoryQuantity() : 0);

//        Cách tiếp cận chính xác (Nâng cao):
//        Để tính toán chính xác fulfillable_quantity, bạn cần một logic phức tạp hơn:
//        Lấy inventory_quantity từ API sản phẩm.
//        Sử dụng API Đơn hàng (GET /admin/orders.json) để lấy danh sách các đơn hàng có trạng thái unfulfilled (chưa hoàn thành).
//        Duyệt qua các đơn hàng đó, cộng tổng số lượng của sản phẩm (SKU) bạn đang xétt
//        Lấy inventory_quantity trừ đi tổng số lượng vừa tính được để ra fulfillable_quantity.
        variant.setFulfillableQuantity(null);
        variant.setWeight(apiVariant.getWeight() != null ? apiVariant.getWeight() : 0.0);
        variant.setWeightUnit(apiVariant.getWeightUnit() != null ? apiVariant.getWeightUnit() : "kg");

        // Convert attributes to JSON
        Map<String, String> attributes = new HashMap<>();
        if (apiVariant.getOption1() != null && !apiVariant.getOption1().isEmpty()) {
            attributes.put("option1", apiVariant.getOption1());
        }
        if (apiVariant.getOption2() != null && !apiVariant.getOption2().isEmpty()) {
            attributes.put("option2", apiVariant.getOption2());
        }
        if (apiVariant.getOption3() != null && !apiVariant.getOption3().isEmpty()) {
            attributes.put("option3", apiVariant.getOption3());
        }
        variant.setAttributes(JsonUtils.toJson(attributes));

        // Warehouse quantities - for now empty, can be extended later
        variant.setWarehouseQuantities("{}");

        return variant;
    }

    public void saveAllProductsSync(List<ProductEntity> products) {
        log.info("Saving {} Sapo products synchronously", products.size());

        if (products.isEmpty()) {
            return;
        }

        try {
            int batchSize = 100;
            for (int i = 0; i < products.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, products.size());
                List<ProductEntity> batch = products.subList(i, endIndex);

                productRepository.saveAll(batch);
                log.info("Saved batch {}-{} of {} products",
                        i + 1, endIndex, products.size());
            }

            log.info("Successfully saved all {} Sapo products", products.size());
        } catch (Exception e) {
            log.error("Failed to save Sapo products synchronously: {}", e.getMessage(), e);
            //throw exception
            //
            throw e;
        }
    }

    public void saveAllVariantsSync(List<ProductVariantEntity> variants) {
        log.info("Saving {} Sapo variants synchronously", variants.size());

        if (variants.isEmpty()) {
            return;
        }

        try {
            int batchSize = 100;
            for (int i = 0; i < variants.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, variants.size());
                List<ProductVariantEntity> batch = variants.subList(i, endIndex);

                productVariantRepository.saveAll(batch);
                log.info("Saved batch {}-{} of {} variants",
                        i + 1, endIndex, variants.size());
            }

            log.info("Successfully saved all {} Sapo variants", variants.size());
        } catch (Exception e) {
            log.error("Failed to save Sapo variants synchronously: {}", e.getMessage(), e);
            throw e;
        }
    }


    private LocalDateTime parseSapoDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }

        try {
            // 1. Dùng Instant để xử lý chuỗi ISO 8601 có 'Z' (Zulu/UTC)
            // Instant.parse() xử lý định dạng "yyyy-MM-ddTHH:mm:ssZ" hoặc có mili giây.
            Instant instant = Instant.parse(dateTimeString.trim());

            // 2. Chuyển Instant (UTC time) sang LocalDateTime (bỏ thông tin múi giờ)
            // Sử dụng ZoneOffset.UTC để đảm bảo chuyển đổi chính xác từ UTC.
            return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);

        } catch (Exception e) {
            // Ghi log chi tiết hơn để dễ debug
            log.warn("[SapoServiceImpl] Failed to parse date time '{}'. Error: {}", dateTimeString, e.getMessage());
            return null;
        }
    }
}
