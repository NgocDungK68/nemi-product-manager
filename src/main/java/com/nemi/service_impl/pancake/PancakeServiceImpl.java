package com.nemi.service_impl.pancake;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.client.PancakeClient;
import com.nemi.configuration.PancakeConfig;
import com.nemi.constant.PancakeConstatns;
import com.nemi.entity.OrderEntity;
import com.nemi.entity.OrderItemEntity;
import com.nemi.entity.PosEntity;
import com.nemi.entity.ProductEntity;
import com.nemi.entity.ProductVariantEntity;
import com.nemi.entity.SyncHistoryEntity;
import com.nemi.enums.PosName;
import com.nemi.enums.PosStatus;
import com.nemi.enums.Status;
import com.nemi.enums.SyncErrorMessage;
import com.nemi.enums.SyncType;
import com.nemi.enums.WeightUnit;
import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.request.pancake.PancakeRequest;
import com.nemi.model.response.PosConnectionResponse;
import com.nemi.model.response.pancake.PancakeOrderResponse;
import com.nemi.model.response.pancake.PancakeProductResponse;
import com.nemi.repository.OrderItemRepository;
import com.nemi.repository.OrderRepository;
import com.nemi.repository.PosRepository;
import com.nemi.repository.ProductRepository;
import com.nemi.repository.ProductVariantRepository;
import com.nemi.repository.SyncHistoryRepository;
import com.nemi.service.GeneralPosService;
import com.nemi.service.PosManagementService;
import com.nemi.util.ClaimUtil;
import com.nemi.util.JsonUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PancakeServiceImpl implements PosManagementService {
    private final ClaimUtil claimUtil;
    private final PancakeClient pancakeClient;
    private final ObjectMapper objectMapper;
    private final PosRepository posRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final SyncHistoryRepository syncHistoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final PancakeConfig pancakeConfig;
    private final ProductVariantRepository productVariantRepository;
    private final GeneralPosService generalPosService;

    private int orderBatchSize;
    private int orderItemBatchSize;
    private int productBatchSize;
    private int pageStartNumber;

    @PostConstruct
    public void init() {
        orderBatchSize = pancakeConfig.getSync().getOrder();
        orderItemBatchSize = pancakeConfig.getSync().getOrderItem();
        productBatchSize = pancakeConfig.getSync().getProduct();
        pageStartNumber = pancakeConfig.getSync().getPageStart();
    }


    @Override
    public String getPosName() {
        return PosName.PANCAKE.getValue();
    }

    @Override
    public PosConnectionResponse connectPos(PosConnectionRequest posConnectionRequest) {
        try {
            String userId = claimUtil.getUserId();
            Map<String, String> configMap = new HashMap<>();
            configMap.put(PancakeConstatns.SHOP_ID, posConnectionRequest.getShopId());

            LocalDateTime expiredTime = LocalDateTime.now().plusYears(1);

            PosEntity posEntityBuilder = PosEntity.builder()
                    .posName(PosName.PANCAKE.name())
                    .userId(userId)
                    .status(PosStatus.ACTIVE.name())
                    .accessToken(posConnectionRequest.getApiKey())
                    .config(JsonUtils.toJson(configMap))
                    .expiredTime(expiredTime)
                    .companyId(String.valueOf(claimUtil.getCompanyId()))
                    .createdBy(claimUtil.getUserName())
                    .build();

            posRepository.save(posEntityBuilder);

            PosConnectionResponse posConnectionResponse = PosConnectionResponse.toPosConnectionResponse(posEntityBuilder);
            log.info("Pancake response is {}", posConnectionResponse);
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
                .syncType(SyncType.PRODUCT.getValue())
                .build();
        try {
            PosEntity posEntity = generalPosService.getPos(posId);

            Map<String, String> configMap = objectMapper.readValue(
                    posEntity.getConfig(), new TypeReference<>() {
                    });
            String shopId = configMap.get(PancakeConstatns.SHOP_ID);
            String accessToken = posEntity.getAccessToken();

            if (StringUtils.isEmpty(shopId) || StringUtils.isEmpty(accessToken)) {
                syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.MISSING_CONFIG, false));
                log.error("Missing required config for posId={}", posId);
                return false;
            }

            List<ProductEntity> allProducts = new ArrayList<>();
            List<ProductVariantEntity> allVariants = new ArrayList<>();
            int pageNumber = pageStartNumber;

            PancakeRequest request = PancakeRequest.builder()
                    .apiKey(posEntity.getAccessToken())
                    .pageNumber(pageNumber)
                    .pageSize(productBatchSize)
                    .shopId(shopId)
                    .build();

            while (true) {
                Optional<PancakeProductResponse> responseOpt = pancakeClient.getProducts(request);
                if (responseOpt.isEmpty()) { // handle tinh huonh nhu server loi
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.PRODUCT_CONNECTION_FAILED, false));
                    log.error("No response from Pancake API when fetching products, posId={}", posId);
                    saveAllProductsSync(allProducts);
                    return false;
                }

                PancakeProductResponse response = responseOpt.get();
                if (!response.isSuccess()) { //hanle cac tinh huon call dc api nhung sai credentail
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.PRODUCT_INVALID_CREDENTIAL, false));
                    log.error("Invalid API key or shopId when fetching products, posId={}", posId);
                    saveAllProductsSync(allProducts);
                    return false;
                }

                if (ObjectUtils.isEmpty(response.getData())) {
                    log.info("No products found with page number: {}", pageNumber);
                    break;
                } else {
                    request.setPageNumber(request.getPageNumber() + 1);
                }

                List<ProductEntity> pageProducts = convertToProductEntities(posId, response.getData());
                allProducts.addAll(pageProducts);

                List<ProductVariantEntity> pageVariants = convertToVariantEntities(posId, response.getData());
                allVariants.addAll(pageVariants);

            }

            syncHistoryRepository.save(toSyncHistory(history, null, true));
            saveAllProductsSync(allProducts);
            saveAllVariantsSync(allVariants);
            log.info("Successfully synced {} products from Pancake", allProducts.size());
            return true;
        } catch (Exception e) {
            log.error("Failed to sync Pancake products - {}", e.getMessage(), e);
            syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.PRODUCT_TECHNICAL_ERROR, false));
            return false;
        }
    }

    public void saveAllProductsSync(List<ProductEntity> products) {
        log.info("Saving {} Pancake products synchronously", products.size());
        if (products.isEmpty()) {
            return;
        }
        try {
            int batchSize = productBatchSize;
            for (int i = 0; i < products.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, products.size());
                List<ProductEntity> batch = products.subList(i, endIndex);
                productRepository.saveAll(batch);
                log.info("Saved batch {}-{} of {} products", i + 1, endIndex, products.size());
            }
            log.info("Successfully saved all {} Pancake products", products.size());
        } catch (Exception e) {
            log.error("Failed to save Pancake products synchronously: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_FAILED));
        }
    }

    private List<ProductEntity> convertToProductEntities(String posId, List<PancakeProductResponse.ProductData> apiProducts) {
        return apiProducts.stream()
                .map(apiProduct -> convertToProductEntity(posId, apiProduct))
                .collect(Collectors.toList());
    }

    public void saveAllVariantsSync(List<ProductVariantEntity> variants) {
        log.info("[NhanhvnServiceImpl.saveAllVariantsSync] Saving {} Nhanh.vn variants synchronously", variants.size());

        if (variants.isEmpty()) {
            log.info("[NhanhvnServiceImpl.saveAllVariantsSync] No variants to save.");
            return;
        }

        try {
            int batchSize = productBatchSize;
            for (int i = 0; i < variants.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, variants.size());
                List<ProductVariantEntity> batch = variants.subList(i, endIndex);

                productVariantRepository.saveAll(batch);
                log.info("[NhanhvnServiceImpl.saveAllVariantsSync] Saved batch {}-{} of {} variants",
                        i + 1, endIndex, variants.size());
            }

            log.info("[NhanhvnServiceImpl.saveAllVariantsSync] Successfully saved all {} Nhanh.vn variants", variants.size());
        } catch (Exception e) {
            log.error("[NhanhvnServiceImpl.saveAllVariantsSync] Failed to save Nhanh.vn variants synchronously: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.DATA_PERSISTENCE_ERROR));
        }
    }


    private ProductEntity convertToProductEntity(String posId, PancakeProductResponse.ProductData apiProducts) {

        String images = Optional.ofNullable(apiProducts.getImages())
                .map(list -> list.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(",")))
                .orElse(null);

        String categories = Optional.ofNullable(apiProducts.getProduct())
                .map(p -> p.getCategories())
                .map(list -> list.stream()
                        .filter(Objects::nonNull)
                        .map(PancakeProductResponse.Category::getName)
                        .collect(Collectors.joining(",")))
                .orElse(null);



        ProductEntity product = ProductEntity.builder()
                .posId(posId)
                .productId(String.valueOf(apiProducts.getId()))
                .code(apiProducts.getProduct().getDisplayId())
                .name(apiProducts.getProduct().getName())
                .productId(apiProducts.getProductId())
                .description(apiProducts.getProduct().getNoteProduct())
                .images(images)
                .category(categories)
                .build();

        if (apiProducts.getIsLocked()) {
            product.setStatus(Status.INACTIVE.getValue());
        } else {
            product.setStatus(Status.ACTIVE.getValue());
        }
        return product;
    }

    private SyncHistoryEntity toSyncHistory(SyncHistoryEntity syncHistoryEntity, SyncErrorMessage syncErrorMessage, Boolean isSyncSuccess) {
        if (!isSyncSuccess) {
            syncHistoryEntity.setEndTime(LocalDateTime.now());
            syncHistoryEntity.setErrorMessage(PosName.PANCAKE.getValue() + ": " + syncErrorMessage.getMessage());
            return syncHistoryEntity;
        }
        syncHistoryEntity.setSyncStatus(PosStatus.SUCCESS.name());
        syncHistoryEntity.setEndTime(LocalDateTime.now());
        return syncHistoryEntity;
    }

    private List<ProductVariantEntity> convertToVariantEntities(String posId, List<PancakeProductResponse.ProductData> apiProducts) {
        return apiProducts.stream()
                .map(apiProduct -> convertToVariantEntity(posId, apiProduct))
                .filter(Objects::nonNull)
                .toList();
    }


    private List<OrderEntity> convertToOrderEntities(String posId, List<PancakeOrderResponse.DataItem> apiOrders) {
        return apiOrders.stream()
                .map(orders -> convertToOrderEntity(posId, orders))
                .filter(Objects::nonNull)
                .toList();
    }

    public OrderEntity convertToOrderEntity(String posId, PancakeOrderResponse.DataItem apiOrders) {
        String status = pancakeConfig.getStatusMapping(apiOrders.getStatus(), apiOrders.getStatusName());
        log.info("status of orderId {} is {}", apiOrders.getId(), status);

        String paymentMethod = Optional.ofNullable(apiOrders.getPaymentPurchaseHistories())
                .filter(histories -> !histories.isEmpty())
                .map(histories -> histories.get(0).getType())
                .orElse(null);

        String orderCode = Optional.ofNullable(apiOrders.getPartner())
                .map(PancakeOrderResponse.Partner::getExtendCode)
                .orElse(null);


        return OrderEntity.builder()
                .posId(posId)
                .orderId(String.valueOf(apiOrders.getId()))
                .orderCode(orderCode)
                .customerName(apiOrders.getShippingAddress().getFullName())
                .customerPhone(apiOrders.getShippingAddress().getPhoneNumber())
                .shippingAddress(apiOrders.getShippingAddress().getFullAddress())
                .paymentMethod(paymentMethod)
                .shippingFee(apiOrders.getShippingFee())
                .totalPrice(apiOrders.getTotalPrice())
                .status(status)
                .createdBy(claimUtil.getUserName())
                .build();
    }

    private List<OrderItemEntity> convertToOrderItemEntities(List<PancakeOrderResponse.DataItem> apiOrders) {
        List<OrderItemEntity> orderItemEntities = new ArrayList<>();
        for (PancakeOrderResponse.DataItem orderData : apiOrders) {
            orderItemEntities.addAll(convertToOrderItemEntity(orderData));
        }
        return orderItemEntities;
    }

    public List<OrderItemEntity> convertToOrderItemEntity(PancakeOrderResponse.DataItem apiOrder) {
        List<OrderItemEntity> orderItemEntities = new ArrayList<>();
        for (PancakeOrderResponse.Item product : apiOrder.getItems()) {
            BigDecimal quantity = BigDecimal.valueOf(product.getQuantity());
            orderItemEntities.add(OrderItemEntity.builder()
                    .orderItemId(String.valueOf(product.getId()))
                    .orderId(String.valueOf(apiOrder.getId()))
                    .quantity(product.getQuantity())
                    .sku(product.getVariationInfo().getDisplayId())
                    .variantName(product.getVariationInfo().getName())
                    .price(product.getVariationInfo().getRetailPrice())
                    .totalPrice(product.getVariationInfo().getRetailPrice().multiply(quantity))
                    .productName(product.getVariationInfo().getName())
                    .createdBy(claimUtil.getUserName())
                    .build());
        }
        return orderItemEntities;
    }

    public ProductVariantEntity convertToVariantEntity(String posId, PancakeProductResponse.ProductData apiProduct) {

        Integer remainQuantity = Optional.ofNullable(apiProduct.getVariationsWarehouses())
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .map(PancakeProductResponse.VariationWarehouse::getRemainQuantity)
                .orElse(null);


        return ProductVariantEntity.builder()
                .variantId(apiProduct.getId())
                .posId(posId)
                .productId(String.valueOf(apiProduct.getProductId()))
                .sku(apiProduct.getDisplayId())
                .barcode(apiProduct.getBarcode())
                .price(BigDecimal.valueOf(apiProduct.getRetailPrice()))
                .inventoryQuantity(apiProduct.getRemainQuantity())
                .fulfillableQuantity(remainQuantity)
                .weight(apiProduct.getWeight())
                .weightUnit(WeightUnit.GAM.getValue())
                .build();
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
                .syncType(SyncType.ORDER.getValue())
                .build();
        try {
            PosEntity posEntity = generalPosService.getPos(posId);

            Map<String, String> configMap = objectMapper.readValue(
                    posEntity.getConfig(), new TypeReference<>() {
                    });
            String shopId = configMap.get(PancakeConstatns.SHOP_ID);
            String accessToken = posEntity.getAccessToken();

            if (StringUtils.isEmpty(shopId) || StringUtils.isEmpty(accessToken)) {
                syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.MISSING_CONFIG, false));
                log.error("Missing required config for posId={}", posId);
                return false;
            }

            int pageNumber = pageStartNumber;
            List<OrderEntity> allOrders = new ArrayList<>();
            List<OrderItemEntity> allOrderItems = new ArrayList<>();

            PancakeRequest request = PancakeRequest.builder()
                    .apiKey(posEntity.getAccessToken())
                    .pageNumber(pageNumber)
                    .pageSize(orderBatchSize)
                    .shopId(shopId)
                    .build();

            while (true) {
                Optional<PancakeOrderResponse> responseOpt = pancakeClient.getOrders(request);
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

                PancakeOrderResponse response = responseOpt.get();
                if (!response.getSuccess()) {
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.ORDER_INVALID_CREDENTIAL, false));
                    log.error("Invalid API key or shopId when fetching orders, posId={}", posId);
                    if (!allOrders.isEmpty()) {
                        saveAllOrdersSync(allOrders);
                    }
                    if (!allOrderItems.isEmpty()) {
                        saveAllOrderItemSync(allOrderItems);
                    }
                    return false;
                }

                List<OrderEntity> pageOrders = convertToOrderEntities(posId, response.getData());
                allOrders.addAll(pageOrders);

                List<OrderItemEntity> pageOrderItems = convertToOrderItemEntities(response.getData());
                allOrderItems.addAll(pageOrderItems);

                if (ObjectUtils.isEmpty(response.getData())) {
                    log.info("No orders found with page number: {}", pageNumber);
                    break;
                } else {
                    request.setPageNumber(request.getPageNumber() + 1);
                }
            }

            saveAllOrdersSync(allOrders);
            saveAllOrderItemSync(allOrderItems);

            syncHistoryRepository.save(toSyncHistory(history, null, true));
            log.info("Successfully synced {} order items  and {} orders from Pancake", allOrderItems.size(), allOrders.size());
            return true;
        } catch (Exception e) {
            log.error("Failed to sync Pancake orders - {}", e.getMessage(), e);
            syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.ORDER_TECHNICAL_ERROR, false));
            return false;
        }
    }
}
