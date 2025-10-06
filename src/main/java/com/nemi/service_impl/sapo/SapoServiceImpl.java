package com.nemi.service_impl.sapo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.client.SapoClient;
import com.nemi.configuration.SapoConfig;
import com.nemi.constant.SapoConstants;
import com.nemi.entity.*;
import com.nemi.enums.PosName;
import com.nemi.enums.PosStatus;
import com.nemi.enums.SyncErrorMessage;
import com.nemi.enums.PosName;
import com.nemi.enums.PosStatus;
import com.nemi.enums.SyncErrorMessage;
import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.request.sapo.SapoRequest;
import com.nemi.model.response.PosConnectionResponse;
import com.nemi.model.response.sapo.SapoAccessTokenResponse;
import com.nemi.model.response.sapo.SapoOrderResponse;
import com.nemi.model.response.sapo.SapoProductResponse;
import com.nemi.model.response.sapo.SapoWebhookResponse;
import com.nemi.repository.OrderItemRepository;
import com.nemi.repository.OrderRepository;
import com.nemi.repository.PosRepository;
import com.nemi.repository.ProductRepository;
import com.nemi.repository.ProductVariantRepository;
import com.nemi.repository.SyncHistoryRepository;
import com.nemi.service.PosManagementService;
import com.nemi.util.ClaimUtil;
import com.nemi.util.JsonUtils;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class SapoServiceImpl implements PosManagementService {

    private final ClaimUtil claimUtil;
    private final SapoClient sapoClient;
    private final ProductRepository productRepository;
    private final PosRepository posRepository;
    private final ProductVariantRepository productVariantRepository;
    private final SyncHistoryRepository syncHistoryRepository;
    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final SapoConfig sapoConfig;
    private int orderBatchSize;
    private int orderItemBatchSize;
    private int productBatchSize;
    private int pageStartNumber;

    @PostConstruct
    public void init() {
        orderBatchSize = sapoConfig.getSync().getOrder();
        orderItemBatchSize = sapoConfig.getSync().getOrderItem();
        productBatchSize = sapoConfig.getSync().getProduct();
        pageStartNumber = sapoConfig.getSync().getPageStart();
    }


    @Override
    public String getPosName() {
        return PosName.SAPO.getValue();
    }

    @Override
    public PosConnectionResponse connectPos(PosConnectionRequest posConnectionRequest) {
        try {
            String userId = claimUtil.getUserId();

            Map<String, String> configMap = new HashMap<>();
            configMap.put(SapoConstants.CLIENT_ID, posConnectionRequest.getClientId());
            configMap.put(SapoConstants.CLIENT_SECRET, posConnectionRequest.getClientSecret());
            configMap.put(SapoConstants.STORE_NAME, posConnectionRequest.getStoreName());


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
            
            // Register webhooks
            List<SapoWebhookResponse> webhooks = sapoClient.registerWebhook(
                    posConnectionRequest.getStoreName(),
                    tokenResponse.getAccessToken(),
                    posEntityBuilder.getId()
            );
            log.info("Registered {} webhooks for POS: {}", webhooks.size(), posEntityBuilder.getId());
            
            PosConnectionResponse posConnectionResponse = PosConnectionResponse.toPosConnectionResponse(posEntityBuilder);
            log.info("Sapo response is {}", posConnectionResponse);
            return posConnectionResponse;
        } catch (Exception e) {
            log.error("Exchange token failed: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_FAILED));
        }
    }

    @Override
    public boolean syncProduct(String posId) {
        SyncHistoryEntity history = SyncHistoryEntity.builder()
                .posId(posId)
                .startTime(LocalDateTime.now())
                .syncStatus(PosStatus.FAIL.name())
                .build();
        try {
            //B1 : Lay posentity va validate posName
            PosEntity posEntity = getPos(posId);

            // B2: parse config
            Map<String, String> configMap = objectMapper.readValue(
                    posEntity.getConfig(),
                    new TypeReference<>() {
                    }
            );
            String clientId = configMap.get(SapoConstants.CLIENT_ID);
            String clientSecret = configMap.get(SapoConstants.CLIENT_SECRET);
            String storeName = configMap.get(SapoConstants.STORE_NAME);
            String accessToken = posEntity.getAccessToken();

            if (clientId == null || clientSecret == null || storeName == null || accessToken == null) {
                syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.MISSING_CONFIG, false));
                log.error("Missing required config for posId={}", posId);
                return false;
            }

            List<ProductEntity> allProducts = new ArrayList<>();
            List<ProductVariantEntity> allVariants = new ArrayList<>();

            //chi set size cho lan dau tien + page-based pagination (Sapo: limit tối đa 250)
            Map<String, Object> paginator = new HashMap<>();
            int limit = 250;
            int page = 1;
            paginator.put("limit", limit);
            paginator.put("page", page);

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

                if (responseOpt.isEmpty()) {
                    log.error("[SapoServiceImpl.syncData] response is empty");
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.TECHNICAL_ERROR, false));
                    return false;
                }

                SapoProductResponse response = responseOpt.get();
                if (response.getProducts() == null || response.getProducts().isEmpty()) {
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

                // xử lý next theo page: tăng page nếu vẫn còn đủ limit (== 250), ngược lại dừng
                if (response.getProducts().size() >= limit) {
                    page++;
                    paginator.put("page", page);
                } else {
                    break; // hết data
                }
            }

            saveAllProductsSync(allProducts);
            saveAllVariantsSync(allVariants);

            syncHistoryRepository.save(toSyncHistory(history, null, true));
            log.info("Successfully synced {} products and {} variants from Sapo", allProducts.size(), allVariants.size());
            return true;
        } catch (Exception e) {
            log.error("Failed to sync Sapo data - {}", e.getMessage(), e);
            syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.TECHNICAL_ERROR, false));
            return false;
        }
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
            product.setCreatedAt(parseSapoDateTime(apiProduct.getCreatedOn()));
        }
        if (apiProduct.getModifiedOn() != null && !apiProduct.getModifiedOn().isEmpty()) {
            product.setUpdatedAt(parseSapoDateTime(apiProduct.getModifiedOn()));
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
        Map<String, String> attributes = getStringStringMap(apiVariant);
        variant.setAttributes(JsonUtils.toJson(attributes));

        // Warehouse quantities - for now empty, can be extended later
        variant.setWarehouseQuantities("{}");

        return variant;
    }

    @NotNull
    private static Map<String, String> getStringStringMap(SapoProductResponse.Variant apiVariant) {
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
        return attributes;
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
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.DATA_PERSISTENCE_ERROR));
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
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.DATA_PERSISTENCE_ERROR));
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

    private SyncHistoryEntity toSyncHistory(SyncHistoryEntity syncHistoryEntity, SyncErrorMessage syncErrorMessage, Boolean isSyncSuccess) {
        if (Boolean.FALSE.equals(isSyncSuccess)) {
            syncHistoryEntity.setEndTime(LocalDateTime.now());
            syncHistoryEntity.setErrorMessage(syncErrorMessage != null ? syncErrorMessage.getMessage() : null);
            return syncHistoryEntity;
        }
        syncHistoryEntity.setSyncStatus(PosStatus.SUCCESS.name());
        syncHistoryEntity.setEndTime(LocalDateTime.now());
        return syncHistoryEntity;
    }

    //-------------------------------------------------------------------------------------------
    private List<OrderEntity> convertToOrderEntities(String posId, List<SapoOrderResponse.Order> apiOrders) {
        return apiOrders.stream()
                .map(orders -> convertToOrderEntity(posId, orders))
                .filter(Objects::nonNull)
                .toList();
    }

    public OrderEntity convertToOrderEntity(String posId, SapoOrderResponse.Order order) {

        Map<String, String> mapping = sapoConfig.getOrder().getStatus().getMapping();
        String status = mapping.getOrDefault(order.getStatus(), "unknown");

        SapoOrderResponse.Fulfillment fulfillment = order.getFulfillments() != null && !order.getFulfillments().isEmpty()
                ? order.getFulfillments().get(0)
                : null;
        SapoOrderResponse.OriginAddress origin = fulfillment != null ? fulfillment.getOriginAddress() : null;

        return OrderEntity.builder()
                .posId(posId)
                .orderId(order.getId().toString())
                .orderCode(order.getName())
                .customerName(origin != null ? origin.getName() : null)
                .customerPhone(origin != null ? origin.getPhone() : null)
                .customerEmail(origin != null ? origin.getEmail() : null)
                .shippingAddress(origin != null
                        ? String.join(", ",
                        Stream.of(origin.getAddress1(), origin.getProvince(), origin.getCity())
                                .filter(Objects::nonNull)
                                .toList())
                        : null)
                .status(status.toUpperCase())
                .paymentMethod(order.getPaymentGatewayNames() != null && !order.getPaymentGatewayNames().isEmpty()
                        ? order.getPaymentGatewayNames().get(0)
                        : null)
                .shippingMethod(fulfillment != null ? fulfillment.getDeliveryMethod() : null)
                .totalPrice(order.getTotalPrice())
                .shippingFee(BigDecimal.ZERO)
                .discountAmount(order.getTotalDiscounts() != null ? order.getTotalDiscounts().doubleValue() : 0.0)
                .build();
    }

    private List<OrderItemEntity> convertToOrderItemEntities(List<SapoOrderResponse.Order> apiOrders) {
        List<OrderItemEntity> orderItemEntities = new ArrayList<>();
        for (SapoOrderResponse.Order orderData : apiOrders) {
            orderItemEntities.addAll(convertToOrderItemEntity(orderData));
        }
        return orderItemEntities;
    }

    public List<OrderItemEntity> convertToOrderItemEntity(SapoOrderResponse.Order apiOrder) {
        List<OrderItemEntity> orderItemEntities = new ArrayList<>();
        for (SapoOrderResponse.LineItem product : apiOrder.getLineItems()) {
            orderItemEntities.add(OrderItemEntity.builder()
                    .orderItemId(String.valueOf(product.getId()))
                    .orderId(String.valueOf(apiOrder.getId()))
                    .quantity(product.getQuantity())
                    .sku(product.getSku())
                    .variantName(product.getVariantTitle())
                    .price(product.getPrice())
                    .totalPrice(product.getTotalDiscount())
                    .productName(product.getName())
                    .fulfillableQuantity(product.getCurrentQuantity())
                    .createdBy(claimUtil.getUserName())
                    .build());
        }
        return orderItemEntities;
    }

    public void saveAllOrdersSync(List<OrderEntity> orderEntities) {
        log.info("Saving {} Pancake orders synchronously", orderEntities.size());
        if (orderEntities.isEmpty()) {
            log.info("No orders to save.");
            return;
        }
        try {
            int batchSize = orderBatchSize;
            for (int i = 0; i < orderEntities.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, orderEntities.size());
                List<OrderEntity> batch = orderEntities.subList(i, endIndex);
                orderRepository.saveAll(batch);
                log.info("Saved batch {}-{} of {} orders", i + 1, endIndex, orderEntities.size());
            }
            log.info("Successfully saved all {} Pancake orders", orderEntities.size());
        } catch (Exception e) {
            log.error("Failed to save Pancake orders synchronously: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.DATA_PERSISTENCE_ERROR));
        }
    }

    public void saveAllOrderItemSync(List<OrderItemEntity> orderItemEntities) {
        log.info("Saving {} Pancake order items synchronously", orderItemEntities.size());
        if (orderItemEntities.isEmpty()) {
            log.info("No order items to save.");
            return;
        }
        try {
            int batchSize = orderItemBatchSize;
            for (int i = 0; i < orderItemEntities.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, orderItemEntities.size());
                List<OrderItemEntity> batch = orderItemEntities.subList(i, endIndex);
                orderItemRepository.saveAll(batch);
                log.info("Saved batch {}-{} of {} order items", i + 1, endIndex, orderItemEntities.size());
            }
            log.info("Successfully saved all {} Pancake order items", orderItemEntities.size());
        } catch (Exception e) {
            log.error("Failed to save Pancake order items synchronously: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.DATA_PERSISTENCE_ERROR));
        }
    }


    @Override
    public boolean syncOrder(String posId) {
        SyncHistoryEntity history = SyncHistoryEntity.builder()
                .posId(posId)
                .startTime(LocalDateTime.now())
                .syncStatus(PosStatus.FAIL.name())
                .build();
        try {
            PosEntity posEntity = getPos(posId);

            Map<String, String> configMap = objectMapper.readValue(
                    posEntity.getConfig(), new TypeReference<>() {
                    });
            String clientId = configMap.get("clientId");
            String clientSecret = configMap.get("clientSecret");
            String storeName = configMap.get("storeName");
            String accessToken = posEntity.getAccessToken();

            if (clientId == null || clientSecret == null || storeName == null || accessToken == null) {
                syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.MISSING_CONFIG, false));
                log.error("Missing required config for posId={}", posId);
                return false;
            }

            int pageNumber = pageStartNumber;
            List<OrderEntity> allOrders = new ArrayList<>();
            List<OrderItemEntity> allOrderItems = new ArrayList<>();

            SapoRequest request = SapoRequest.builder()
                    .limit(orderBatchSize)
                    .page(pageNumber)
                    .storeName(storeName)
                    .accessToken(accessToken)
                    .build();

            while (true) {
                Optional<SapoOrderResponse> responseOpt = sapoClient.getOrders(request);
                if (responseOpt.isEmpty()) {
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.ORDER_CONNECTION_FAILED, false));
                    log.error("No response from Pancake API when fetching orders, posId={}", posId);
                    if (!allOrders.isEmpty()) {
                        saveAllOrdersSync(allOrders);
                    }
                    if (!allOrderItems.isEmpty()) {
                        saveAllOrderItemSync(allOrderItems);
                    }
                    return false;
                }

                SapoOrderResponse response = responseOpt.get();

                if (response.getOrders() == null || response.getOrders().isEmpty()) {
                    log.info("No orders found with page number: {}", pageNumber);
                    break;
                } else {
                    request.setPage(request.getPage() + 1);
                }

                List<OrderEntity> pageOrders = convertToOrderEntities(posId, response.getOrders());
                allOrders.addAll(pageOrders);

                List<OrderItemEntity> pageOrderItems = convertToOrderItemEntities(response.getOrders());
                allOrderItems.addAll(pageOrderItems);

            }

            saveAllOrdersSync(allOrders);
            saveAllOrderItemSync(allOrderItems);

            syncHistoryRepository.save(toSyncHistory(history, null, true));
            log.info("Successfully synced {} orders from Pancake", allOrders.size());
            log.info("Successfully synced {} order items from Pancake", allOrderItems.size());
            return true;
        } catch (Exception e) {
            log.error("Failed to sync Pancake orders - {}", e.getMessage(), e);
            syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.ORDER_TECHNICAL_ERROR, false));
            return false;
        }
    }
}
