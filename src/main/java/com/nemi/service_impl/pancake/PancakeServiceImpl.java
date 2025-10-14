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
import com.nemi.enums.SyncErrorMessage;
import com.nemi.enums.SyncType;
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
import com.nemi.repository.jdbc.OrderItemJdbcRepository;
import com.nemi.repository.jdbc.OrderJdbcRepository;
import com.nemi.repository.jdbc.ProductJdbcRepository;
import com.nemi.repository.jdbc.ProductVariantJdbcRepository;
import com.nemi.service.GeneralPosService;
import com.nemi.service.PosManagementService;
import com.nemi.util.ClaimUtil;
import com.nemi.util.JsonUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
    private final PancakeAsyncService pancakeAsyncService;
    private final ProductJdbcRepository productJdbcRepository;
    private final ProductVariantJdbcRepository productVariantJdbcRepository;
    private final OrderJdbcRepository orderJdbcRepository;
    private final OrderItemJdbcRepository orderItemJdbcRepositoryl;

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
    @Async("syncExecutor")
    public CompletableFuture<Boolean> syncProduct(String posId) {
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
                CompletableFuture.completedFuture(false);
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
                    productJdbcRepository.insertProductsVariantParallel(allProducts);
                    return CompletableFuture.completedFuture(false);
                }

                PancakeProductResponse response = responseOpt.get();
                if (!response.isSuccess()) { //hanle cac tinh huon call dc api nhung sai credentail
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.PRODUCT_INVALID_CREDENTIAL, false));
                    log.error("Invalid API key or shopId when fetching products, posId={}", posId);
                    productJdbcRepository.insertProductsVariantParallel(allProducts);
                    CompletableFuture.completedFuture(false);
                }

                if (ObjectUtils.isEmpty(response.getData())) {
                    log.info("No products found with page number: {}", pageNumber);
                    break;
                } else {
                    request.setPageNumber(request.getPageNumber() + 1);
                }

                CompletableFuture<List<ProductEntity>> pageProducts = pancakeAsyncService.convertToProductEntities(posId, response.getData());
                CompletableFuture<List<ProductVariantEntity>> pageVariants = pancakeAsyncService.convertToVariantEntities(posId, response.getData());

                CompletableFuture.allOf(pageProducts, pageVariants).join();

                List<ProductEntity> productEntityList = pageProducts.join();
                List<ProductVariantEntity> productVariantEntityList = pageVariants.join();

                allProducts.addAll(productEntityList);
                allVariants.addAll(productVariantEntityList);

            }
            Long startDate = System.currentTimeMillis();
            log.info("Start date {}", startDate);
            syncHistoryRepository.save(toSyncHistory(history, null, true));
            productJdbcRepository.insertProductsVariantParallel(allProducts);
            productVariantJdbcRepository.insertProductsVariantParallel(allVariants);
            Long endDate = System.currentTimeMillis();
            log.info("End date {}", endDate);
            log.info("Successfully synced {} products from Pancake", allProducts.size());
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Failed to sync Pancake products - {}", e.getMessage(), e);
            syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.PRODUCT_TECHNICAL_ERROR, false));
            return CompletableFuture.completedFuture(false);
        }
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
            String userName = claimUtil.getUserName();
            while (true) {
                Optional<PancakeOrderResponse> responseOpt = pancakeClient.getOrders(request);
                if (responseOpt.isEmpty()) {
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.ORDER_CONNECTION_FAILED, false));
                    log.error("No response from Pancake API when fetching orders, posId={}", posId);
                    if (!allOrders.isEmpty()) {
                        orderJdbcRepository.insertOrdersParallel(allOrders);
                    }
                    if (!allOrderItems.isEmpty()) {
                        orderItemJdbcRepositoryl.insertOrderItemParallel(allOrderItems);
                    }
                    return false;
                }

                PancakeOrderResponse response = responseOpt.get();
                if (!response.getSuccess()) {
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.ORDER_INVALID_CREDENTIAL, false));
                    log.error("Invalid API key or shopId when fetching orders, posId={}", posId);
                    if (!allOrders.isEmpty()) {
                        orderJdbcRepository.insertOrdersParallel(allOrders);
                    }
                    if (!allOrderItems.isEmpty()) {
                        orderItemJdbcRepositoryl.insertOrderItemParallel(allOrderItems);
                    }
                    return false;
                }

                if (ObjectUtils.isEmpty(response.getData())) {
                    log.info("No orders found with page number: {}", pageNumber);
                    break;
                } else {
                    request.setPageNumber(request.getPageNumber() + 1);
                }

                CompletableFuture<List<OrderEntity>> pageOrders = pancakeAsyncService.convertToOrderEntities(posId, response.getData(), userName);
                CompletableFuture<List<OrderItemEntity>> pageOrderItems = pancakeAsyncService.convertToOrderItemEntities(response.getData(), userName);

                CompletableFuture.allOf(pageOrders, pageOrderItems).join();
                List<OrderEntity> pageOrderList = pageOrders.join();
                List<OrderItemEntity> pageOrderItemList = pageOrderItems.join();

                allOrders.addAll(pageOrderList);
                allOrderItems.addAll(pageOrderItemList);


            }

            orderJdbcRepository.insertOrdersParallel(allOrders);
            orderItemJdbcRepositoryl.insertOrderItemParallel(allOrderItems);

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
