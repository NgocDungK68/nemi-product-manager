package com.nemi.service_impl.pancake;

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
import com.nemi.mapper.PancakeMapper;
import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.request.pancake.PancakeRequest;
import com.nemi.model.response.PosConnectionResponse;
import com.nemi.model.response.pancake.PancakeOrderResponse;
import com.nemi.model.response.pancake.PancakeProductResponse;
import com.nemi.repository.PosRepository;
import com.nemi.repository.SyncHistoryRepository;
import com.nemi.repository.jdbc.OrderItemJdbcRepository;
import com.nemi.repository.jdbc.OrderJdbcRepository;
import com.nemi.repository.jdbc.ProductJdbcRepository;
import com.nemi.repository.jdbc.ProductVariantJdbcRepository;
import com.nemi.service.EncryptionService;
import com.nemi.service.GeneralPosService;
import com.nemi.service.PosManagementService;
import com.nemi.util.ClaimUtil;
import com.nemi.util.JsonUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
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
    private final PosRepository posRepository;
    private final SyncHistoryRepository syncHistoryRepository;
    private final PancakeConfig pancakeConfig;
    private final GeneralPosService generalPosService;
    private final PancakeMapper pancakeMapper;
    private final ProductJdbcRepository productJdbcRepository;
    private final ProductVariantJdbcRepository productVariantJdbcRepository;
    private final OrderJdbcRepository orderJdbcRepository;
    private final OrderItemJdbcRepository orderItemJdbcRepositoryl;
    private final EncryptionService encryptionService;

    private int productBatchSize;
    private int pageStartNumber;

    @PostConstruct
    public void init() {
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
         String departmentId = claimUtil.getDepartmentId();
            Map<String, String> configMap = new HashMap<>();
            configMap.put(PancakeConstatns.SHOP_ID, posConnectionRequest.getShopId());

            String webhookToken = generalPosService.generateWebhookToken(posConnectionRequest.getShopId());


            PosEntity posEntityBuilder = PosEntity.builder()
                    .posName(PosName.PANCAKE.getValue())
                    .userId(userId)
                    .status(PosStatus.ACTIVE.name())
                    .accessToken(encryptionService.encrypt(posConnectionRequest.getApiKey()))
                    .config(encryptionService.encrypt(JsonUtils.toJson(configMap)))
                    .companyId(String.valueOf(claimUtil.getCompanyId()))
                    .departmentId(claimUtil.getDepartmentId())
                    .createdBy(claimUtil.getUserName())
                    .webhookToken(encryptionService.encrypt(webhookToken))
                    .departmentId(departmentId)
                    .build();

            posRepository.save(posEntityBuilder);

            String webhookUrl = generalPosService.creatWebhookUrl(posEntityBuilder.getId(), PosName.PANCAKE.getValue());
            String keyValue = generalPosService.creatKeyValueMap(PancakeConstatns.WEBHOOK_TOKEN, webhookToken);

            PosConnectionResponse posConnectionResponse = PosConnectionResponse.toPosConnectionResponse(posEntityBuilder);
            posConnectionResponse.setWebhookToken(webhookToken);
            posConnectionResponse.setWebhookUrl(webhookUrl);
            posConnectionResponse.setKeyValue(keyValue);
            posConnectionResponse.setDepartmentId(departmentId);

            log.info("Pancake response is {}", posConnectionResponse);
            return posConnectionResponse;
        } catch (Exception e) {
            log.error("Exchange token failed: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_FAILED));
        }
    }

    @Override
    @Async("syncExecutor")
    public void syncProduct(String posId,String departmentId) {
        SyncHistoryEntity history = SyncHistoryEntity.builder()
                .posId(posId)
                .startTime(LocalDateTime.now())
                .syncStatus(PosStatus.FAIL.name())
                .syncType(SyncType.PRODUCT.getValue())
                .build();
        try {
            PosEntity posEntity = generalPosService.getPos(posId);

            String decryptedToken = encryptionService.decrypt(posEntity.getAccessToken());

            // Decrypt config before using
            String decryptedConfig = encryptionService.decrypt(posEntity.getConfig());
            PancakeRequest request = PancakeRequest.buildRequest
                    (decryptedConfig,
                            decryptedToken,
                            pageStartNumber,
                            productBatchSize,
                            posEntity.getCreatedAt()
                    );

            List<ProductEntity> allProducts = new ArrayList<>();
            List<ProductVariantEntity> allVariants = new ArrayList<>();

            String userName = claimUtil.getUserName();

            while (true) {

                Optional<PancakeProductResponse> responseOpt = pancakeClient.getProducts(request);
                if (responseOpt.isEmpty()) { // handle tinh huonh nhu server loi
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.PRODUCT_CONNECTION_FAILED, false));
                    log.error("No response from Pancake API when fetching products, posId={}", posId);
                    productJdbcRepository.insertProductsParallel(allProducts);
                    productVariantJdbcRepository.insertProductsVariantParallel(allVariants);

                }

                PancakeProductResponse response = responseOpt.get();
                if (!response.isSuccess()) { //hanle cac tinh huon call dc api nhung sai credentail
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.PRODUCT_INVALID_CREDENTIAL, false));
                    log.error("Invalid API key or shopId when fetching products, posId={}", posId);
                    productJdbcRepository.insertProductsParallel(allProducts);
                    productVariantJdbcRepository.insertProductsVariantParallel(allVariants);
                    CompletableFuture.completedFuture(false);
                }

                if (ObjectUtils.isEmpty(response.getData())) {
                    log.info("No products found with page number: {}", request.getPageNumber());
                    break;
                } else {
                    request.setPageNumber(request.getPageNumber() + 1);
                }

                List<ProductEntity> pageProducts = pancakeMapper.convertToProductEntities(posId, response.getData(), userName, departmentId);
                List<ProductVariantEntity> pageVariants = pancakeMapper.convertToVariantEntities(posId, response.getData(),userName);


                allProducts.addAll(pageProducts);
                allVariants.addAll(pageVariants);

            }
            Long startDate = System.currentTimeMillis();
            log.info("Start date {}", startDate);
            syncHistoryRepository.save(toSyncHistory(history, null, true));
            productJdbcRepository.insertProductsParallel(allProducts);
            productVariantJdbcRepository.insertProductsVariantParallel(allVariants);
            Long endDate = System.currentTimeMillis();
            log.info("End date {}", endDate);
            log.info("Successfully synced {} products from Pancake", allProducts.size());

        } catch (Exception e) {
            log.error("Failed to sync Pancake products - {}", e.getMessage(), e); //500 ki tu
            syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.PRODUCT_TECHNICAL_ERROR, false));

        }
    }

    private SyncHistoryEntity toSyncHistory(SyncHistoryEntity syncHistoryEntity, SyncErrorMessage syncErrorMessage, Boolean isSyncSuccess) {
        if (Boolean.FALSE.equals(isSyncSuccess)) {
            syncHistoryEntity.setEndTime(LocalDateTime.now());
            syncHistoryEntity.setErrorMessage(PosName.PANCAKE.getValue() + ": " + syncErrorMessage.getMessage());
            return syncHistoryEntity;
        }
        syncHistoryEntity.setSyncStatus(PosStatus.SUCCESS.name());
        syncHistoryEntity.setEndTime(LocalDateTime.now());
        return syncHistoryEntity;
    }


    @Override
    @Async("syncExecutor")
    public void syncOrder(String posId, String departmentId) {
        SyncHistoryEntity history = SyncHistoryEntity.builder()
                .posId(posId)
                .startTime(LocalDateTime.now())
                .syncStatus(PosStatus.FAIL.name())
                .syncType(SyncType.ORDER.getValue())
                .build();
        try {
            PosEntity posEntity = generalPosService.getPos(posId);
            String decryptedToken = encryptionService.decrypt(posEntity.getAccessToken());

            // Decrypt config before using
            String decryptedConfig = encryptionService.decrypt(posEntity.getConfig());
            PancakeRequest request = PancakeRequest.buildRequest(decryptedConfig, decryptedToken, pageStartNumber, productBatchSize, posEntity.getCreatedAt());

            List<OrderEntity> allOrders = new ArrayList<>();
            List<OrderItemEntity> allOrderItems = new ArrayList<>();
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
                }

                if (ObjectUtils.isEmpty(response.getData())) {
                    log.info("No orders found with page number: {}", request.getPageNumber());
                    break;
                } else {
                    request.setPageNumber(request.getPageNumber() + 1);
                }

                List<OrderEntity> pageOrders = pancakeMapper.convertToOrderEntities(posId, response.getData(), userName, departmentId);
                List<OrderItemEntity> pageOrderItems = pancakeMapper.convertToOrderItemEntities(response.getData(), userName);

                allOrders.addAll(pageOrders);
                allOrderItems.addAll(pageOrderItems);

            }

            orderJdbcRepository.insertOrdersParallel(allOrders);
            orderItemJdbcRepositoryl.insertOrderItemParallel(allOrderItems);

            syncHistoryRepository.save(toSyncHistory(history, null, true));
            log.info("Successfully synced {} order items  and {} orders from Pancake", allOrderItems.size(), allOrders.size());

        } catch (Exception e) {
            log.error("Failed to sync Pancake orders - {}", e.getMessage(), e);
            syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.ORDER_TECHNICAL_ERROR, false));

        }
    }
}
