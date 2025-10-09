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
import com.nemi.repository.*;
import com.nemi.service.GeneralPosService;
import com.nemi.service.PosManagementService;
import com.nemi.util.ClaimUtil;
import com.nemi.util.JsonUtils;
import com.nemi.utils.PosUtils;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.util.CollectionUtils;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
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
    private final GeneralPosService generalPosService;
    private int orderBatchSize;
    private int orderItemBatchSize;
    private int productBatchSize;
    private int pageStartNumber;
    private int productLimit;

    @PostConstruct
    public void init() {
        orderBatchSize = sapoConfig.getSync().getOrder();
        orderItemBatchSize = sapoConfig.getSync().getOrderItem();
        productBatchSize = sapoConfig.getSync().getProduct();
        pageStartNumber = sapoConfig.getSync().getPageStart();
        productLimit = sapoConfig.getSync().getProductLimit();
    }


    @Override
    public String getPosName() {
        return PosName.SAPO.getValue();
    }

    @Override
    public PosConnectionResponse connectPos(PosConnectionRequest posConnectionRequest) {
        try {


            Map<String, String> configMap = new HashMap<>();
            configMap.put(SapoConstants.CLIENT_ID, posConnectionRequest.getClientId());
            configMap.put(SapoConstants.CLIENT_SECRET, posConnectionRequest.getClientSecret());
            configMap.put(SapoConstants.STORE_NAME, posConnectionRequest.getStoreName());


            SapoAccessTokenResponse tokenResponse = sapoClient.getAccessToken(posConnectionRequest);
            if (ObjectUtils.isEmpty(tokenResponse.getAccessToken())) {
                log.error("Sapo response does not contain accessToken: {}", tokenResponse);
            }

            PosEntity newPos = createNewPos(tokenResponse, configMap);
            PosConnectionResponse posConnectionResponse = PosConnectionResponse.toPosConnectionResponse(newPos);

            // Register webhooks
            List<SapoWebhookResponse> webhooks = sapoClient.registerWebhook(
                    posConnectionRequest.getStoreName(),
                    tokenResponse.getAccessToken(),
                    newPos.getId()
            );
            log.info("Registered {} webhooks for POS: {}", webhooks.size(), newPos.getId());

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
            PosEntity posEntity = generalPosService.getPos(posId);
            // B2: parse config
            SapoRequest request = buildRequest(posEntity);

            if (isInvalidRequest(request)) {
                syncHistoryRepository.save(toSyncHistory(history, (SyncErrorMessage.MISSING_CONFIG), false));
                log.error("[NhanhvnServiceImpl.syncProduct] Missing required config for posId={}", posId);
                return false;
            }


            List<ProductEntity> allProducts = new ArrayList<>();
            List<ProductVariantEntity> allVariants = new ArrayList<>();

            //chi set size cho lan dau tien + page-based pagination (Sapo: limit tối đa 250)
            SapoRequest.Paginator paginator = new SapoRequest.Paginator();

            paginator.setLimit(productLimit);
            paginator.setPage(pageStartNumber);

            //B3 : goi SapoClient de lay du lieu
            while (true) {
                Optional<SapoProductResponse> responseOpt = sapoClient.getProducts(request);

                if (ObjectUtils.isEmpty(responseOpt)) {
                    log.error("[SapoServiceImpl.syncData] response is empty");
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.TECHNICAL_ERROR, false));
                    return false;
                }

                SapoProductResponse response = responseOpt.get();
                if (ObjectUtils.isEmpty(response.getProducts())) {

                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.CONNECTION_FAILED, false));
                    log.info("[SapoServiceImpl.syncProduct] No products found with paginator: {}", paginator);
                    return false;
                }

                // Convert products and variants
                for (SapoProductResponse.Product sapoProduct : response.getProducts()) {
                    ProductEntity productEntity = convertToProductEntity(posId, sapoProduct);
                    allProducts.add(productEntity);

                    // Convert variants
                    if (ObjectUtils.isNotEmpty(sapoProduct.getVariants())) {
                        List<ProductVariantEntity> variants = convertToVariantEntities(posId, productEntity.getProductId(), sapoProduct.getVariants());
                        allVariants.addAll(variants);
                    }
                }
                log.info("Fetched {} products and {} variants", response.getProducts().size(), allVariants.size());

                // xử lý next theo page: tăng page nếu vẫn còn đủ limit (== 250), ngược lại dừng
                if (response.getProducts().size() >= productLimit) {
                    pageStartNumber++;
                    paginator.setPage(pageStartNumber);
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
        product.setStatus(apiProduct.getStatus().toUpperCase());
        product.setImages(JsonUtils.toJson(apiProduct.getImages().stream()
                .map(SapoProductResponse.Image::getSrc) // Dùng method reference
                .collect(Collectors.toList())));

        // Set timestamps - parse from string format
        if (ObjectUtils.isEmpty(apiProduct.getCreatedOn())) {
            product.setCreatedAt(PosUtils.parseDateTime(apiProduct.getCreatedOn()));
        }
        if (ObjectUtils.isEmpty(apiProduct.getModifiedOn())) {
            product.setUpdatedAt(PosUtils.parseDateTime(apiProduct.getModifiedOn()));
        }
        return product;
    }

    private List<ProductVariantEntity> convertToVariantEntities(String posId, String productId, List<SapoProductResponse.Variant> apiVariants) {
        return apiVariants.stream()
                .map(apiVariant -> convertToVariantEntity(posId, productId, apiVariant))
                .collect(Collectors.toList());
    }

    private ProductVariantEntity convertToVariantEntity(String posId, String productId, SapoProductResponse.Variant apiVariant) {
        ProductVariantEntity variant = new ProductVariantEntity();

        // Required fields
        variant.setVariantId(String.valueOf(apiVariant.getId()));
        variant.setPosId(posId);
        variant.setProductId(productId);

        // Handle nullable fields with defaults
        variant.setSku(apiVariant.getSku());
        variant.setBarcode(apiVariant.getBarcode());

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
        if (ObjectUtils.isNotEmpty(apiVariant.getOption1())) {
            attributes.put("option1", apiVariant.getOption1());
        }
        if (ObjectUtils.isNotEmpty(apiVariant.getOption2())) {
            attributes.put("option2", apiVariant.getOption2());
        }
        if (ObjectUtils.isEmpty(apiVariant.getOption3())) {
            attributes.put("option3", apiVariant.getOption3());
        }
        return attributes;
    }

    public void saveAllProductsSync(List<ProductEntity> products) {
        log.info("Saving {} Sapo products synchronously", products.size());

        //Vì danh sách rỗng là trường hợp hợp lệ, không phải lỗi. Khi phân trang, trang cuối cùng có thể trả về 0 sản phẩm; ta
        // chỉ no-op thay vì fail toàn bộ sync.
        //Nếu ném exception ở đây, cả quy trình đồng bộ sẽ bị đánh dấu lỗi dù hệ thống hoạt động đúng (không còn dữ liệu để lưu).
        if (ObjectUtils.isEmpty(products)) {
            return;
        }

        try {
            int batchSize = productBatchSize;
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

        if (ObjectUtils.isEmpty(variants)) {
            return;
        }

        try {
            int batchSize = productBatchSize;
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
        String status = sapoConfig.getStatusMapping(order.getStatus());

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
        if (ObjectUtils.isEmpty(apiOrder) || CollectionUtils.isEmpty(apiOrder.getLineItems())) {
            return orderItemEntities;
        }

        for (SapoOrderResponse.LineItem product : apiOrder.getLineItems()) {
            if (ObjectUtils.isEmpty(product)) {
                continue;
            }

            BigDecimal price = product.getPrice();
            int quantity = product.getQuantity();
            BigDecimal totalPrice = price.multiply(BigDecimal.valueOf(quantity));

            orderItemEntities.add(OrderItemEntity.builder()
                    .orderItemId(String.valueOf(product.getId()))
                    .orderId(String.valueOf(apiOrder.getId()))
                    .quantity(quantity)
                    .sku(product.getSku())
                    .variantName(product.getVariantTitle())
                    .price(price)
                    .totalPrice(totalPrice)
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
            PosEntity posEntity = generalPosService.getPos(posId);

            Map<String, String> configMap = objectMapper.readValue(
                    posEntity.getConfig(), new TypeReference<>() {
                    });

            String clientId = configMap.get(SapoConstants.CLIENT_ID);
            String clientSecret = configMap.get(SapoConstants.CLIENT_SECRET);
            String storeName = configMap.get(SapoConstants.STORE_NAME);
            String accessToken = posEntity.getAccessToken();

            if (StringUtils.isEmpty(clientId)|| StringUtils.isEmpty(clientSecret)   ||StringUtils.isEmpty(storeName)||StringUtils.isEmpty(accessToken)) {
                syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.MISSING_CONFIG, false));
                log.error("Missing required config for posId={}", posId);
                return false;
            }

            int pageNumber = pageStartNumber;
            List<OrderEntity> allOrders = new ArrayList<>();
            List<OrderItemEntity> allOrderItems = new ArrayList<>();

            // Initialize paginator similar to syncProduct
            SapoRequest.Paginator paginator = new SapoRequest.Paginator();
            paginator.setLimit(productLimit); // reuse productLimit as API max page size
            paginator.setPage(pageNumber);

            SapoRequest request = SapoRequest.builder()
                    .storeName(storeName)
                    .accessToken(accessToken)
                    .paginator(paginator)
                    .build();

            while (true) {
                Optional<SapoOrderResponse> responseOpt = sapoClient.getOrders(request);
                if (ObjectUtils.isEmpty(responseOpt)) {
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.ORDER_CONNECTION_FAILED, false));
                    log.error("No response from Sapo API when fetching orders, posId={}", posId);
                    break;
                }

                SapoOrderResponse response = responseOpt.get();
                if (ObjectUtils.isEmpty(response.getOrders())) {
                    log.info("No orders found with paginator: page={}, limit={}", paginator.getPage(), paginator.getLimit());
                    break;
                }

                List<OrderEntity> pageOrders = convertToOrderEntities(posId, response.getOrders());
                allOrders.addAll(pageOrders);

                List<OrderItemEntity> pageOrderItems = convertToOrderItemEntities(response.getOrders());
                allOrderItems.addAll(pageOrderItems);

                // Continue pagination: increment page; stop if returned less than limit
                if (response.getOrders().size() >= paginator.getLimit()) {
                    paginator.setPage(paginator.getPage() + 1);
                } else {
                    break;
                }
            }

            if (ObjectUtils.isNotEmpty(allOrders)) {
                saveAllOrdersSync(allOrders);
            }
            if (ObjectUtils.isNotEmpty(allOrderItems)) {
                saveAllOrderItemSync(allOrderItems);
            }

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
    private PosEntity createNewPos(SapoAccessTokenResponse tokenResponse, Map<String, String> configMap) {
        PosEntity newPos = PosEntity.builder()
                .posName(PosName.SAPO.getValue())
                .accessToken(tokenResponse.getAccessToken())
                .status(PosStatus.ACTIVE.name())
                .config(JsonUtils.toJson(configMap))
                .companyId(claimUtil.getCompanyId() != null ? String.valueOf(claimUtil.getCompanyId()) : "default")
                .userId(claimUtil.getUserId() != null ? claimUtil.getUserId() : "system")
                .createdBy(claimUtil.getUserName() != null ? claimUtil.getUserName() : "system")
                .build();

        return posRepository.save(newPos);
    }

    public SapoRequest buildRequest(PosEntity posEntity) {
        try {
            //parse config
            Map<String, String> configMap = objectMapper.readValue(
                    posEntity.getConfig(),
                    new TypeReference<>() {
                    }
            );

            String clientId = configMap.get(SapoConstants.CLIENT_ID);
            String clientSecret = configMap.get(SapoConstants.CLIENT_SECRET);
            String storeName = configMap.get(SapoConstants.STORE_NAME);
            String accessToken = posEntity.getAccessToken();


            return SapoRequest.builder()
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .storeName(storeName)
                    .accessToken(accessToken)
                    .build();
        } catch (Exception e) {
            log.error("Failed request PancakeRequest - {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.JSON_PARSE_ERROR));
        }
    }

    private boolean isInvalidRequest(SapoRequest request) {
        return ObjectUtils.isEmpty(request.getClientId())
                || ObjectUtils.isEmpty(request.getClientSecret())
                || ObjectUtils.isEmpty(request.getAccessToken());
    }
}
