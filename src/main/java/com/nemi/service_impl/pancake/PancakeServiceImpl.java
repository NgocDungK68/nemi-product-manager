package com.nemi.service_impl.pancake;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.client.NhanhvnClient;
import com.nemi.client.PancakeClient;
import com.nemi.configuration.PancakeConfig;
import com.nemi.constant.enums.OrderStatus;
import com.nemi.constant.enums.PosName;
import com.nemi.constant.enums.PosStatus;
import com.nemi.constant.enums.SyncErrorMessage;
import com.nemi.entity.OrderEntity;
import com.nemi.entity.OrderItemEntity;
import com.nemi.entity.PosEntity;
import com.nemi.entity.ProductEntity;
import com.nemi.entity.SyncHistoryEntity;
import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.request.nhanhvn.NhanhvnRequest;
import com.nemi.model.request.pancake.PancakeRequest;
import com.nemi.model.response.PosConnectionResponse;
import com.nemi.model.response.nhanhvn.NhanhvnAccessTokenResponse;
import com.nemi.model.response.nhanhvn.NhanhvnOrderResponse;
import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;
import com.nemi.model.response.pancake.PancakeOrderResponse;
import com.nemi.model.response.pancake.PancakeProductResponse;
import com.nemi.repository.OrderItemRepository;
import com.nemi.repository.OrderRepository;
import com.nemi.repository.PosRepository;
import com.nemi.repository.ProductRepository;
import com.nemi.repository.SyncHistoryRepository;
import com.nemi.service.AbstractPosManagementService;
import com.nemi.service.PosManagementService;
import com.nemi.util.ClaimUtil;
import com.nemi.util.JsonUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class PancakeServiceImpl  implements PosManagementService {
    private final ClaimUtil claimUtil;
    private final PancakeClient pancakeClient;
    private final ObjectMapper objectMapper;
    private final PosRepository posRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final SyncHistoryRepository syncHistoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final PancakeConfig pancakeConfig;


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
            log.info("Nhanhvn response is {}", posConnectionResponse);
            return posConnectionResponse;
        } catch (Exception e) {
            log.error("Exchange token failed: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_FAILED));
        }
    }

    @Override
    public boolean syncData(String posId) {
        SyncHistoryEntity history = SyncHistoryEntity.builder()
                .posId(posId)
                .startTime(LocalDateTime.now())
                .syncStatus(PosStatus.FAIL.name())
                .build();
        try {
            // B1: lấy PosEntity và validate posName
            PosEntity posEntity = getPos(posId);

            // B2: parse config
            Map<String, String> configMap = objectMapper.readValue(
                    posEntity.getConfig(),
                    new TypeReference<>() {
                    }
            );
            String shopId = configMap.get("shopId");
            String accessToken = posEntity.getAccessToken();

            if (shopId == null || accessToken == null) {
                syncHistoryRepository.save(toSyncHistory(history, (SyncErrorMessage.MISSING_CONFIG), false));
                log.error("Missing required config for posId={}", posId); // throw techial
                throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.DATA_INVALID));
            }

            List<ProductEntity> allProducts = new ArrayList<>();
//--------------------------------------------------------------------------------------
            // chỉ set size cho lần đầu
            int pageSize = 50;
            int pageNumber = 1;
            PancakeRequest request = PancakeRequest.builder()
                    .apiKey(posEntity.getAccessToken())
                    .pageNumber(pageNumber)
                    .pageSize(pageSize)
                    .shopId(shopId)
                    .build();

            while (true) {
                Optional<PancakeProductResponse> responseOpt = pancakeClient.getProducts(request);

                if (responseOpt.isEmpty()) {
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.CONNECTION_FAILED, false));
                    log.error("Missing required config for posId={}", posId);
                    throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_FAILED));
                }

                PancakeProductResponse response = responseOpt.get();

                if (!response.isSuccess()) {
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.CONNECTION_FAILED, false));
                    log.error("Missing required config for posId={}", posId);
                    throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_FAILED));
                }

                List<ProductEntity> pageProducts = convertToProductEntities(posId, response.getData());
                allProducts.addAll(pageProducts);

//                if (response.getData() == null || response.getData().isEmpty()) {
                if (response.getData() == null || response.getData().isEmpty()) {
                    log.info("No products found with page number: {}",pageNumber);
                    break;
                } else {
                    request.setPageNumber(request.getPageNumber()+1);
                }



                log.info("Fetched {} products, total so far: {}", pageProducts.size(), allProducts.size());

            }


            syncHistoryRepository.save(toSyncHistory(history, null, true));
            saveAllProductsSync(allProducts);

            log.info("Successfully synced {} products from Nhanh.vn", allProducts.size());
            return true;

        } catch (Exception e) {
            log.error("Failed to sync Nhanh.vn data - {}", e.getMessage(), e);
            syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.TECHNICAL_ERROR, false));
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_FAILED));
        }
    }


    public void saveAllProductsSync(List<ProductEntity> products) {
        log.info("Saving {} Nhanh.vn products synchronously", products.size());

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

            log.info("Successfully saved all {} Nhanh.vn products", products.size());
        } catch (Exception e) {
            log.error("Failed to save Nhanh.vn products synchronously: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_FAILED));
        }
    }

    public PosEntity getPos(String posId) {
        log.debug("[NhanhvnSyncDataImpl.getPos] posId: {}", posId);

        // Lấy PosEntity từ DB
        return posRepository.findById(posId)
                .orElseThrow(() -> {
                    log.error("Error [NhanhvnSyncDataImpl.getPos] not found posId: {}", posId);
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
        if(apiProducts.getIsLocked()) {
            product.setStatus(OrderStatus.CANCELLED.getValue());
        } else {
            product.setStatus(OrderStatus.PROCESSING.getValue());
        }

        return product;
    }

    private SyncHistoryEntity toSyncHistory(SyncHistoryEntity syncHistoryEntity, SyncErrorMessage syncErrorMessage, Boolean isSyncSuccess) {
        if (isSyncSuccess == false) {
            syncHistoryEntity.setEndTime(LocalDateTime.now());
            syncHistoryEntity.setErrorMessage( PosName.PANCAKE.getValue() + ": "+ syncErrorMessage.getMessage());
            return syncHistoryEntity;
        }
        syncHistoryEntity.setSyncStatus(PosStatus.SUCCESS.name());
        syncHistoryEntity.setEndTime(LocalDateTime.now());
        return syncHistoryEntity;
    }

    //------------------------------------------------------------------------------------------
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


        // pancake khong co customer email
        return OrderEntity.builder()
                .posId(posId)
                .orderId(String.valueOf(apiOrders.getId()))
                .orderCode(orderCode)
                .customerName(apiOrders.getShippingAddress().getFullName())
                .customerPhone(apiOrders.getShippingAddress().getPhoneNumber())// khi user co du thi them custemer phone va email
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
        for(PancakeOrderResponse.DataItem orderData : apiOrders){
            orderItemEntities.addAll(convertToOrderItemEntity(orderData));
        }
        return  orderItemEntities;
    }
    public List<OrderItemEntity> convertToOrderItemEntity(PancakeOrderResponse.DataItem apiOrder) {
        List<OrderItemEntity> orderItemEntities = new ArrayList<>();
        for(PancakeOrderResponse.Item product : apiOrder.getItems()){
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
        log.info("Saving {} Nhanh.vn order synchronously", orderEntities.size());

        if (orderEntities.isEmpty()) {
            log.info("No products to save.");
            return;
        }

        try {
            int batchSize = 50;
            for (int i = 0; i < orderEntities.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, orderEntities.size());
                List<OrderEntity> batch = orderEntities.subList(i, endIndex);

                orderRepository.saveAll(batch);
                log.info("Saved batch {}-{} of {} products",
                        i + 1, endIndex, orderEntities.size());
            }

            log.info("Successfully saved all {} Nhanh.vn products", orderEntities.size());
        } catch (Exception e) {
            log.error("Failed to save Nhanh.vn products synchronously: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.DATA_PERSISTENCE_ERROR));
        }
    }

    public void saveAllOrderItemSync(List<OrderItemEntity> orderItemEntities) {
        log.info("Saving {} Nhanh.vn order item synchronously", orderItemEntities.size());

        if (orderItemEntities.isEmpty()) {
            log.info("No products to save.");
            return;
        }

        try {
            int batchSize = 50;
            for (int i = 0; i < orderItemEntities.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, orderItemEntities.size());
                List<OrderItemEntity> batch = orderItemEntities.subList(i, endIndex);

                orderItemRepository.saveAll(batch);
                log.info("Saved batch {}-{} of {} products",
                        i + 1, endIndex, orderItemEntities.size());
            }

            log.info("Successfully saved all {} Nhanh.vn order item", orderItemEntities.size());
        } catch (Exception e) {
            log.error("Failed to save Nhanh.vn products synchronously: {}", e.getMessage(), e);
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
            // B1: lấy PosEntity và validate posName
            PosEntity posEntity = getPos(posId);

            // B2: parse config
            Map<String, String> configMap = objectMapper.readValue(
                    posEntity.getConfig(),
                    new TypeReference<>() {
                    }
            );

            String shopId = configMap.get("shopId");
            String accessToken = posEntity.getAccessToken();

            if (shopId == null || accessToken == null) {
                syncHistoryRepository.save(toSyncHistory(history, (SyncErrorMessage.MISSING_CONFIG), false));
                log.error("Missing required config for posId={}", posId); // throw techial
                throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.DATA_INVALID));
            }

            int pageSize = 50;
            int pageNumber = 1;

            List<OrderEntity> allOrders = new ArrayList<>();
            List<OrderItemEntity> allOrderItems = new ArrayList<>();

            // set size mỗi pag

            PancakeRequest request = PancakeRequest.builder()
                    .apiKey(posEntity.getAccessToken())
                    .pageNumber(pageNumber)
                    .pageSize(pageSize)
                    .shopId(shopId)
                    .build();

            while (true) {
                Optional<PancakeOrderResponse> responseOpt = pancakeClient.getOrders(request);

                if (responseOpt.isEmpty()) {
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.CONNECTION_FAILED, false));
                    log.error("Missing required config for posId={}", posId);
                    if(allOrders.size() > 0){
                        saveAllOrdersSync(allOrders);
                    }
                    if (allOrderItems.size() > 0){
                        saveAllOrderItemSync(allOrderItems);
                    }
                    throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_FAILED));
                }
                PancakeOrderResponse response = responseOpt.get();
                if (!response.getSuccess()) {
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.CONNECTION_FAILED, false));
                    log.error("Missing required config for posId={}", posId);
                    if(allOrders.size() > 0){
                        saveAllOrdersSync(allOrders);
                    }
                    if (allOrderItems.size() > 0){
                        saveAllOrderItemSync(allOrderItems);
                    }
                    throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_FAILED));
                }

                List<OrderEntity> pageOrders = convertToOrderEntities(posId, response.getData());
                allOrders.addAll(pageOrders);

                List<OrderItemEntity> pageOrderItems = convertToOrderItemEntities(response.getData());
                allOrderItems.addAll(pageOrderItems);
                if (request.getPageNumber() > 2) {

                    log.info("No products found with page number: {}",pageNumber);
                    break;
                } else {
                    request.setPageNumber(request.getPageNumber()+1);
                }

            }

            saveAllOrdersSync(allOrders);
            saveAllOrderItemSync(allOrderItems);
            syncHistoryRepository.save(toSyncHistory(history, null, true));

            log.info("Successfully synced {} products from Pancake", allOrders.size());
            log.info("Successfully synced {} variants from Pancake", allOrderItems.size());
            return true;

        } catch (Exception e) {
            log.error("Failed to sync Nhanh.vn data order - {}", e.getMessage(), e);
            syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.TECHNICAL_ERROR, false));
            return false;
        }
    }



}
