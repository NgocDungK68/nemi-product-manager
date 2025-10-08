package com.nemi.service_impl.nhanhvn;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.client.NhanhvnClient;
import com.nemi.configuration.NhanhvnConfig;
import com.nemi.constant.NhanhvnConstants;
import com.nemi.entity.*;
import com.nemi.enums.PosName;
import com.nemi.enums.PosStatus;
import com.nemi.enums.SyncErrorMessage;
import com.nemi.enums.WeightUnit;
import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.request.nhanhvn.NhanhvnRequest;
import com.nemi.model.response.PosConnectionResponse;
import com.nemi.model.response.nhanhvn.NhanhvnAccessTokenResponse;
import com.nemi.model.response.nhanhvn.NhanhvnOrderResponse;
import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;
import com.nemi.repository.*;
import com.nemi.service.GeneralPosService;
import com.nemi.service.PosManagementService;
import com.nemi.util.ClaimUtil;
import com.nemi.util.JsonUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class NhanhvnServiceImpl implements PosManagementService {
    private final ClaimUtil claimUtil;
    private final NhanhvnClient nhanhvnClient;
    private final ObjectMapper objectMapper;
    private final ProductRepository productRepository;
    private final PosRepository posRepository;
    private final ProductVariantRepository productVariantRepository;
    private final SyncHistoryRepository syncHistoryRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final NhanhvnConfig nhanhvnConfig;
    private final GeneralPosService generalPosService;

    private int orderBatchSize;
    private int orderItemBatchSize;
    private int productBatchSize;
    private int pageSize;

    @PostConstruct
    public void init() {
        orderBatchSize = nhanhvnConfig.getSync().getOrder();
        orderItemBatchSize = nhanhvnConfig.getSync().getOrderItem();
        productBatchSize = nhanhvnConfig.getSync().getProduct();
        pageSize = nhanhvnConfig.getSync().getPageSize();
    }

    @Override
    public String getPosName() {
        return PosName.NHANHVN.getValue();
    }

    @Override
    public PosConnectionResponse connectPos(PosConnectionRequest posConnectionRequest) {
        try {
            NhanhvnAccessTokenResponse tokenResponse = nhanhvnClient.getAccessToken(posConnectionRequest);
            if (ObjectUtils.isEmpty(tokenResponse.getData()) || ObjectUtils.isEmpty(tokenResponse.getData().getAccessToken())) {
                log.error("[NhanhvnServiceImpl.connectPos] Nhanhvn response is null, stop persist to db {}", tokenResponse);
                throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_FAILED));
            }

            Map<String, String> configMap = buildConfigMap(posConnectionRequest);
            LocalDateTime expiredTime = Instant.ofEpochSecond(tokenResponse.getData().getExpiredAt())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            PosEntity newPos = createNewPos(tokenResponse, configMap, expiredTime);
            PosConnectionResponse posConnectionResponse = PosConnectionResponse.toPosConnectionResponse(newPos);

            log.info("[NhanhvnServiceImpl.connectPos] Connected successfully: {}", newPos.getId());
            return posConnectionResponse;
        } catch (Exception e) {
            log.error("[NhanhvnServiceImpl.connectPos] Exchange token failed: {}", e.getMessage(), e);
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
            // lấy PosEntity và validate posName
            PosEntity posEntity = generalPosService.getPos(posId);
            NhanhvnRequest request = buildRequest(posEntity);

            if (isInvalidRequest(request)) {
                syncHistoryRepository.save(toSyncHistory(history, (SyncErrorMessage.MISSING_CONFIG), false));
                log.error("[NhanhvnServiceImpl.syncProduct] Missing required config for posId={}", posId);
                return false;
            }

            List<ProductEntity> allProducts = new ArrayList<>();
            List<ProductVariantEntity> allVariants = new ArrayList<>();

            // set size mỗi page
            NhanhvnRequest.Paginator paginator = new NhanhvnRequest.Paginator();
            paginator.setSize(pageSize);
            request.setPaginator(paginator);

            while (true) {
                Optional<NhanhvnProductResponse> responseOpt = nhanhvnClient.getProducts(request);

                if (responseOpt.isEmpty()) {
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.TECHNICAL_ERROR, false));
                    log.error("[NhanhvnServiceImpl.syncProduct] Missing required config for posId={}," +
                            " Failed to fetch products with paginator: {}", posId, paginator);
                    throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_FAILED));
                }

                NhanhvnProductResponse response = responseOpt.get();

                if (ObjectUtils.isEmpty(response.getData())) {
                    if (response.getCode() == NhanhvnConstants.SUCCESS_CODE) {
                        break;
                    }
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.CONNECTION_FAILED, false));
                    log.info("[NhanhvnServiceImpl.syncProduct] No products found with paginator: {}", paginator);
                    throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_FAILED));
                }

                // product
                List<ProductEntity> pageProducts = convertToProductEntities(posId, response.getData());
                allProducts.addAll(pageProducts);
                log.info("[NhanhvnServiceImpl.syncProduct] Fetched {} products, total so far: {}", pageProducts.size(), allProducts.size());

                // variant
                List<ProductVariantEntity> pageVariants = convertToVariantEntities(posId, response.getData());
                allVariants.addAll(pageVariants);
                log.info("[NhanhvnServiceImpl.syncProduct] Fetched {} variants, total so far: {}", pageVariants.size(), allVariants.size());

                // xử lý next
                if (ObjectUtils.isNotEmpty(response.getPaginator()) && ObjectUtils.isNotEmpty(response.getPaginator().getNext())) {
                    paginator.setNext(response.getPaginator().getNext());
                } else {
                    break; // hết data
                }
            }
            syncHistoryRepository.save(toSyncHistory(history, null, true));
            saveAllProductsSync(allProducts);
            saveAllVariantsSync(allVariants);

            log.info("[NhanhvnServiceImpl.syncProduct] Successfully synced {} products and {} variants from Nhanh.vn", allProducts.size(), allVariants.size());
            return true;

        } catch (Exception e) {
            log.error("[NhanhvnServiceImpl.syncProduct] Failed to sync Nhanh.vn data - {}", e.getMessage(), e);
            syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.TECHNICAL_ERROR, false));
            return false;
        }
    }

    public void saveAllProductsSync(List<ProductEntity> products) {
        log.info("[NhanhvnServiceImpl.saveAllProductsSync] Saving {} Nhanh.vn products synchronously", products.size());

        if (products.isEmpty()) {
            log.info("[NhanhvnServiceImpl.saveAllProductsSync] No products to save.");
            return;
        }

        try {
            int batchSize = productBatchSize;
            for (int i = 0; i < products.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, products.size());
                List<ProductEntity> batch = products.subList(i, endIndex);

                productRepository.saveAll(batch);
                log.info("[NhanhvnServiceImpl.saveAllProductsSync] Saved batch {}-{} of {} products",
                        i + 1, endIndex, products.size());
            }

            log.info("[NhanhvnServiceImpl.saveAllProductsSync] Successfully saved all {} Nhanh.vn products", products.size());
        } catch (Exception e) {
            log.error("[NhanhvnServiceImpl.saveAllProductsSync] Failed to save Nhanh.vn products synchronously: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.DATA_PERSISTENCE_ERROR));
        }
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

    private List<ProductEntity> convertToProductEntities(String posId, List<NhanhvnProductResponse.ProductData> apiProducts) {
        return apiProducts.stream()
                .map(apiProduct -> convertToProductEntity(posId, apiProduct))
                .filter(Objects::nonNull)
                .toList();
    }

    public ProductEntity convertToProductEntity(String posId, NhanhvnProductResponse.ProductData apiProduct) {
        if (!(apiProduct.getParentId()).equals(NhanhvnConstants.PARENT_PRODUCT)) return null;

        int statusCode = apiProduct.getStatus();
        Map<Integer, String> mapping = nhanhvnConfig.getProduct().getStatus().getMapping();
        String status = mapping.getOrDefault(statusCode, "unknown");

        return ProductEntity.builder()
                .posId(posId)
                .productId(String.valueOf(apiProduct.getId()))
                .code(apiProduct.getCode())
                .name(apiProduct.getName())
                .status(status)
                .build();
    }

    private List<ProductVariantEntity> convertToVariantEntities(String posId, List<NhanhvnProductResponse.ProductData> apiProducts) {
        return apiProducts.stream()
                .map(apiProduct -> convertToVariantEntity(posId, apiProduct))
                .filter(Objects::nonNull)
                .toList();
    }

    public ProductVariantEntity convertToVariantEntity(String posId, NhanhvnProductResponse.ProductData apiProduct) {
        if ((apiProduct.getParentId()).equals(NhanhvnConstants.PARENT_PRODUCT)) return null;
        return ProductVariantEntity.builder()
                .variantId(String.valueOf(apiProduct.getId()))
                .posId(posId)
                .productId(String.valueOf(apiProduct.getParentId()))
                .sku(apiProduct.getCode())
                .barcode(apiProduct.getBarcode())
                .price(BigDecimal.valueOf(apiProduct.getPrices().getRetail()))
                .inventoryQuantity(apiProduct.getInventory().getRemain())
                .fulfillableQuantity(apiProduct.getInventory().getAvailable())
                .weight(apiProduct.getShipping().getWeight())
                .weightUnit(WeightUnit.GAM.getValue())
                .build();
    }

    //------------------------------------------------------------------------------------------
    private List<OrderEntity> convertToOrderEntities(String posId, List<NhanhvnOrderResponse.OrderData> apiOrders) {
        return apiOrders.stream()
                .map(apiOrder -> convertToOrderEntity(posId, apiOrder))
                .filter(Objects::nonNull)
                .toList();
    }

    public OrderEntity convertToOrderEntity(String posId, NhanhvnOrderResponse.OrderData apiOrder) {

        int statusCode = apiOrder.getInfo().getStatus();
        Map<Integer, String> mapping = nhanhvnConfig.getOrder().getStatus().getMapping();
        String status = mapping.getOrDefault(statusCode, "unknown");


        return OrderEntity.builder()
                .posId(posId)
                .orderId(String.valueOf(apiOrder.getInfo().getId()))
                .orderCode(apiOrder.getCarrier().getCarrierCode())
                .customerName(apiOrder.getShippingAddress().getName())
                .customerEmail(apiOrder.getShippingAddress().getEmail())
                .customerPhone(apiOrder.getShippingAddress().getMobile())// khi user co du thi them custemer phone va email
                .shippingAddress(apiOrder.getShippingAddress().getAddress())
                .shippingMethod(apiOrder.getCarrier().getName())
                .paymentMethod(apiOrder.getPayment().getBusinessPayment().toString())
                .shippingFee(apiOrder.getCarrier().getShipFee())
                .totalPrice(totalProductPrice(apiOrder))
                .status(status)
                .createdBy(claimUtil.getUserName())
                .build();
    }

    private BigDecimal totalProductPrice(NhanhvnOrderResponse.OrderData apiOrders) {
        BigDecimal totalPrice = BigDecimal.valueOf(0);
        for (NhanhvnOrderResponse.Product product : apiOrders.getProducts()) {
            BigDecimal price = product.getPrice(); // BigDecimal
            BigDecimal vat = product.getVat().divide(BigDecimal.valueOf(100));
            BigDecimal quantity = BigDecimal.valueOf(product.getQuantity());
            BigDecimal discount = product.getDiscount();


            BigDecimal lineTotal = price
                    .multiply(BigDecimal.ONE.add(vat))
                    .multiply(quantity)
                    .subtract(ObjectUtils.isNotEmpty(discount) ? discount : BigDecimal.ZERO);

            totalPrice = totalPrice.add(lineTotal);
        }
        return totalPrice.add(apiOrders.getCarrier().getShipFee());

    }

    private List<OrderItemEntity> convertToOrderItemEntities(List<NhanhvnOrderResponse.OrderData> apiOrders) {
        List<OrderItemEntity> orderItemEntities = new ArrayList<>();
        for (NhanhvnOrderResponse.OrderData orderData : apiOrders) {
            orderItemEntities.addAll(convertToOrderItemEntity(orderData));
        }
        return orderItemEntities;
    }

    public List<OrderItemEntity> convertToOrderItemEntity(NhanhvnOrderResponse.OrderData apiOrder) {
        List<OrderItemEntity> orderItemEntities = new ArrayList<>();
        for (NhanhvnOrderResponse.Product product : apiOrder.getProducts()) {
            BigDecimal quantity = BigDecimal.valueOf(product.getQuantity());
            orderItemEntities.add(OrderItemEntity.builder()
                    .orderItemId(String.valueOf(product.getId()))
                    .orderId(apiOrder.getChannel().getAppOrderId())
                    .quantity(product.getQuantity())
                    .sku(product.getImeiId())
                    .price(product.getPrice())
                    .totalPrice(product.getPrice().multiply(quantity))
                    .productName(product.getName())
                    .createdBy(claimUtil.getUserName())
                    .build());
        }

        return orderItemEntities;
    }

    public void saveAllOrdersSync(List<OrderEntity> orderEntities) {
        log.info("[NhanhvnServiceImpl.saveAllOrdersSync] Saving {} Nhanh.vn orders synchronously", orderEntities.size());

        if (orderEntities.isEmpty()) {
            log.info("[NhanhvnServiceImpl.saveAllOrdersSync] No order to save.");
            return;
        }

        try {
            int batchSize = orderBatchSize;
            for (int i = 0; i < orderEntities.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, orderEntities.size());
                List<OrderEntity> batch = orderEntities.subList(i, endIndex);

                orderRepository.saveAll(batch);
                log.info("[NhanhvnServiceImpl.saveAllOrdersSync] Saved batch {}-{} of {} orders",
                        i + 1, endIndex, orderEntities.size());
            }

            log.info("[NhanhvnServiceImpl.saveAllOrdersSync] Successfully saved all {} Nhanh.vn orders", orderEntities.size());
        } catch (Exception e) {
            log.error("[NhanhvnServiceImpl.saveAllOrdersSync] Failed to save Nhanh.vn orders synchronously: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.DATA_PERSISTENCE_ERROR));
        }
    }

    public void saveAllOrderItemSync(List<OrderItemEntity> orderItemEntities) {
        log.info("[NhanhvnServiceImpl.saveAllOrderItemSync] Saving {} Nhanh.vn order item synchronously", orderItemEntities.size());

        if (orderItemEntities.isEmpty()) {
            log.info("[NhanhvnServiceImpl.saveAllOrderItemSync] No order item to save.");
            return;
        }

        try {
            int batchSize = orderItemBatchSize;
            for (int i = 0; i < orderItemEntities.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, orderItemEntities.size());
                List<OrderItemEntity> batch = orderItemEntities.subList(i, endIndex);

                orderItemRepository.saveAll(batch);
                log.info("[NhanhvnServiceImpl.saveAllOrderItemSync] Saved batch {}-{} of {} order items",
                        i + 1, endIndex, orderItemEntities.size());
            }

            log.info("[NhanhvnServiceImpl.saveAllOrderItemSync] Successfully saved all {} Nhanh.vn order items", orderItemEntities.size());
        } catch (Exception e) {
            log.error("[NhanhvnServiceImpl.saveAllOrderItemSync] Failed to save Nhanh.vn order items synchronously: {}", e.getMessage(), e);
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
            // lấy PosEntity và validate posName
            PosEntity posEntity = generalPosService.getPos(posId);
            NhanhvnRequest request = buildRequest(posEntity);

            if (isInvalidRequest(request)) {
                syncHistoryRepository.save(toSyncHistory(history, (SyncErrorMessage.MISSING_CONFIG), false));
                log.error("[NhanhvnServiceImpl.syncOrder] Missing required config for posId={}", posId); // throw techial
                return false;
            }

            List<OrderEntity> allOrders = new ArrayList<>();
            List<OrderItemEntity> allOrderItems = new ArrayList<>();

            // set size mỗi page
            NhanhvnRequest.Paginator paginator = new NhanhvnRequest.Paginator();
            paginator.setSize(pageSize);
            request.setPaginator(paginator);

            while (true) {
                Optional<NhanhvnOrderResponse> responseOpt = nhanhvnClient.getOrders(request);

                if (responseOpt.isEmpty()) {
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.TECHNICAL_ERROR, false));
                    log.error("[NhanhvnServiceImpl.syncOrder] Missing required config for posId={}," +
                            " Failed to fetch products with paginator: {}", posId, paginator);
                    return false;
                }

                NhanhvnOrderResponse response = responseOpt.get();

                if (ObjectUtils.isEmpty(response.getData())) {
                    if (response.getCode() == NhanhvnConstants.SUCCESS_CODE) {
                        break;
                    }
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.CONNECTION_FAILED, false));
                    log.info("[NhanhvnServiceImpl.syncOrder] No order found with paginator: {}", paginator);
                    return false;
                }

                // order
                List<OrderEntity> pageOrders = convertToOrderEntities(posId, response.getData());
                allOrders.addAll(pageOrders);
                log.info("[NhanhvnServiceImpl.syncOrder] Fetched {} orders, total so far: {}", pageOrders.size(), pageOrders.size());


                List<OrderItemEntity> pageOrderItem = convertToOrderItemEntities(response.getData());
                allOrderItems.addAll(pageOrderItem);
                log.info("[NhanhvnServiceImpl.syncOrder] Fetched {} order items, total so far: {}", pageOrderItem.size(), pageOrderItem.size());

                // xử lý next
                if (ObjectUtils.isNotEmpty(response.getPaginator()) && ObjectUtils.isNotEmpty(response.getPaginator().getNext())) {
                    paginator.setNext(response.getPaginator().getNext());
                } else {
                    break; // hết data
                }
            }

            saveAllOrdersSync(allOrders);
            saveAllOrderItemSync(allOrderItems);
            syncHistoryRepository.save(toSyncHistory(history, null, true));

            log.info("[NhanhvnServiceImpl.syncOrder] Successfully synced {} orders and {} order items from Nhanh.vn", allOrders.size(), allOrderItems.size());
            return true;

        } catch (Exception e) {
            log.error("[NhanhvnServiceImpl.syncOrder] Failed to sync Nhanh.vn data order - {}", e.getMessage(), e);
            syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.TECHNICAL_ERROR, false));
            return false;
        }
    }


    private SyncHistoryEntity toSyncHistory(SyncHistoryEntity syncHistoryEntity, SyncErrorMessage syncErrorMessage, Boolean isSyncSuccess) {
        if (Boolean.FALSE.equals(isSyncSuccess)) {
            syncHistoryEntity.setEndTime(LocalDateTime.now());
            syncHistoryEntity.setErrorMessage(syncErrorMessage.getMessage());
            return syncHistoryEntity;
        }
        syncHistoryEntity.setSyncStatus(PosStatus.SUCCESS.name());
        syncHistoryEntity.setEndTime(LocalDateTime.now());
        return syncHistoryEntity;
    }

    private PosEntity createNewPos(NhanhvnAccessTokenResponse tokenResponse,
                                   Map<String, String> configMap,
                                   LocalDateTime expiredTime) {
        PosEntity newPos = PosEntity.builder()
                .posName(PosName.NHANHVN.getValue())
                .userId(claimUtil.getUserId())
                .status(PosStatus.ACTIVE.name())
                .accessToken(tokenResponse.getData().getAccessToken())
                .config(JsonUtils.toJson(configMap))
                .expiredTime(expiredTime)
                .companyId(String.valueOf(claimUtil.getCompanyId()))
                .createdBy(claimUtil.getUserName())
                .build();

        return posRepository.save(newPos);
    }

    private Map<String, String> buildConfigMap(PosConnectionRequest request) {
        Map<String, String> map = new HashMap<>();
        map.put(NhanhvnConstants.SECRET_ID, request.getAppSecret());
        map.put(NhanhvnConstants.APP_ID, request.getAppId());
        map.put(NhanhvnConstants.BUSINESS_ID, request.getBusinessId());
        return map;
    }

    public NhanhvnRequest buildRequest(PosEntity posEntity) {
        try {
            // parse config
            Map<String, String> configMap = objectMapper.readValue(
                    posEntity.getConfig(),
                    new TypeReference<>() {
                    }
            );

            String appId = configMap.get(NhanhvnConstants.APP_ID);
            String businessId = configMap.get(NhanhvnConstants.BUSINESS_ID);
            String accessToken = posEntity.getAccessToken();

            return NhanhvnRequest.builder()
                    .appId(appId)
                    .businessId(businessId)
                    .accessToken(accessToken)
                    .build();
        } catch (Exception e) {
            log.error("[NhanhvnServiceImpl.buildRequest] Build Nhanh.vn request failed: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.JSON_PARSE_ERROR));
        }
    }

    private boolean isInvalidRequest(NhanhvnRequest request) {
        return ObjectUtils.isEmpty(request.getAppId())
                || ObjectUtils.isEmpty(request.getBusinessId())
                || ObjectUtils.isEmpty(request.getAccessToken());
    }
}