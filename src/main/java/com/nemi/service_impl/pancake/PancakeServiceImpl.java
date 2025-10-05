package com.nemi.service_impl.pancake;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.client.PancakeClient;
import com.nemi.configuration.PancakeConfig;
import com.nemi.enums.BatchSize;
import com.nemi.enums.OrderStatus;
import com.nemi.enums.PosName;
import com.nemi.enums.PosStatus;
import com.nemi.enums.SyncErrorMessage;
import com.nemi.entity.OrderEntity;
import com.nemi.entity.OrderItemEntity;
import com.nemi.entity.PosEntity;
import com.nemi.entity.ProductEntity;
import com.nemi.entity.SyncHistoryEntity;
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
import com.nemi.repository.SyncHistoryRepository;
import com.nemi.service.PosManagementService;
import com.nemi.util.ClaimUtil;
import com.nemi.util.JsonUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
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
            configMap.put("shopId", posConnectionRequest.getShopId());

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
                .build();
        try {
            PosEntity posEntity = getPos(posId);

            Map<String, String> configMap = objectMapper.readValue(
                    posEntity.getConfig(), new TypeReference<>() {
                    });
            String shopId = configMap.get("shopId");
            String accessToken = posEntity.getAccessToken();

            if (shopId == null || accessToken == null) {
                syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.MISSING_CONFIG, false));
                log.error("Missing required config for posId={}", posId);
                return false;
            }

            List<ProductEntity> allProducts = new ArrayList<>();
            int pageNumber = pageStartNumber;

            PancakeRequest request = PancakeRequest.builder()
                    .apiKey(posEntity.getAccessToken())
                    .pageNumber(pageNumber)
                    .pageSize(productBatchSize)
                    .shopId(shopId)
                    .build();

            while (true) {
                Optional<PancakeProductResponse> responseOpt = pancakeClient.getProducts(request);
                if (responseOpt.isEmpty()) {
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.PRODUCT_CONNECTION_FAILED, false));
                    log.error("No response from Pancake API when fetching products, posId={}", posId);
                    saveAllProductsSync(allProducts);
                    return false;
                }

                PancakeProductResponse response = responseOpt.get();
                if (!response.isSuccess()) {
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.PRODUCT_INVALID_CREDENTIAL, false));
                    log.error("Invalid API key or shopId when fetching products, posId={}", posId);
                    saveAllProductsSync(allProducts);
                    return false;
                }

                List<ProductEntity> pageProducts = convertToProductEntities(posId, response.getData());
                allProducts.addAll(pageProducts);

                if (response.getData() == null || response.getData().isEmpty()) {
                    log.info("No products found with page number: {}", pageNumber);
                    break;
                } else {
                    request.setPageNumber(request.getPageNumber() + 1);
                }
                log.info("Fetched {} products, total so far: {}", pageProducts.size(), allProducts.size());
            }

            syncHistoryRepository.save(toSyncHistory(history, null, true));
            saveAllProductsSync(allProducts);
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

    public PosEntity getPos(String posId) {
        log.debug("[PancakeSyncDataImpl.getPos] posId: {}", posId);
        return posRepository.findById(posId)
                .orElseThrow(() -> {
                    log.error("Error [PancakeSyncDataImpl.getPos] not found posId: {}", posId);
                    return new TechnicalException(AlertMessages.alert(TechnicalAlertCode.DATA_INVALID));
                });
    }

    private List<ProductEntity> convertToProductEntities(String posId, List<PancakeProductResponse.ProductData> apiProducts) {
        return apiProducts.stream()
                .map(apiProduct -> convertToProductEntity(posId, apiProduct))
                .collect(Collectors.toList());
    }

    private ProductEntity convertToProductEntity(String posId, PancakeProductResponse.ProductData apiProducts) {
        ProductEntity product = new ProductEntity();
        product.setPosId(posId);
        product.setProductId(String.valueOf(apiProducts.getId()));
        product.setCode(apiProducts.getProductId());
        product.setName(apiProducts.getProduct().getName());
        product.setProductId(apiProducts.getId());
        if (apiProducts.getIsLocked()) {
            product.setStatus(OrderStatus.CANCELLED.getValue());
        } else {
            product.setStatus(OrderStatus.PROCESSING.getValue());
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

    private List<OrderEntity> convertToOrderEntities(String posId, List<PancakeOrderResponse.DataItem> apiOrders) {
        return apiOrders.stream()
                .map(orders -> convertToOrderEntity(posId, orders))
                .filter(Objects::nonNull)
                .toList();
    }

    public OrderEntity convertToOrderEntity(String posId, PancakeOrderResponse.DataItem apiOrders) {
        String status = Optional.ofNullable(apiOrders.getStatus())
                .map(code -> pancakeConfig.getOrder().getStatus().getMapping()
                        .getOrDefault(code, "unknown"))
                .orElse(apiOrders.getStatusName());

        String paymentMethod = Optional.ofNullable(apiOrders.getPaymentPurchaseHistories())
                .filter(histories -> !histories.isEmpty())
                .map(histories -> histories.get(0).getType())
                .orElse("unknown");

        String orderCode = Optional.ofNullable(apiOrders.getPartner())
                .map(PancakeOrderResponse.Partner::getExtendUpdate)
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0).getTrackingId())
                .orElse("unknown");

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
                .status(status.toUpperCase())
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
                    .sku(product.getVariationId())
                    .variantName(product.getVariationInfo().getName())
                    .price(product.getVariationInfo().getRetailPrice())
                    .totalPrice(product.getVariationInfo().getRetailPrice().multiply(quantity))
                    .productName(product.getVariationInfo().getName())
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
            String shopId = configMap.get("shopId");
            String accessToken = posEntity.getAccessToken();

            if (shopId == null || accessToken == null) {
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

                if (request.getPageNumber() > 2) {
                    log.info("No orders found with page number: {}", pageNumber);
                    break;
                } else {
                    request.setPageNumber(request.getPageNumber() + 1);
                }
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
