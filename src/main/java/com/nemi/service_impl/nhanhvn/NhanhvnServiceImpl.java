package com.nemi.service_impl.nhanhvn;

import com.nemi.client.NhanhvnClient;
import com.nemi.configuration.NhanhvnConfig;
import com.nemi.constant.NhanhvnConstants;
import com.nemi.constant.PosConstants;
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
import com.nemi.mapper.NhanhvnMapper;
import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.request.nhanhvn.NhanhvnRequest;
import com.nemi.model.response.PosConnectionResponse;
import com.nemi.model.response.nhanhvn.NhanhvnAccessTokenResponse;
import com.nemi.model.response.nhanhvn.NhanhvnOrderResponse;
import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;
import com.nemi.repository.OrderItemRepository;
import com.nemi.repository.OrderRepository;
import com.nemi.repository.PosRepository;
import com.nemi.repository.ProductRepository;
import com.nemi.repository.ProductVariantRepository;
import com.nemi.repository.SyncHistoryRepository;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class NhanhvnServiceImpl implements PosManagementService {
    private final ClaimUtil claimUtil;
    private final NhanhvnClient nhanhvnClient;
    private final ProductRepository productRepository;
    private final PosRepository posRepository;
    private final ProductVariantRepository variantRepository;
    private final SyncHistoryRepository syncHistoryRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final NhanhvnConfig nhanhvnConfig;
    private final GeneralPosService generalPosService;
    private final NhanhvnMapper nhanhvnMapper;
    private final EncryptionService encryptionService;

    private int batchSize;
    private int pageSize;

    @PostConstruct
    public void init() {
        batchSize = nhanhvnConfig.getSync().getBatchSize();
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
    @Async("syncExecutor")
    public void syncProduct(String posId) {
        SyncHistoryEntity history = SyncHistoryEntity.builder()
                .posId(posId)
                .startTime(LocalDateTime.now())
                .syncStatus(PosStatus.FAIL.name())
                .syncType(SyncType.PRODUCT.getValue())
                .build();
        try {
            String username = claimUtil.getUserName();
            // lấy PosEntity và validate posName
            PosEntity posEntity = generalPosService.getPos(posId);
            
            // Decrypt access token before using for API calls
            String decryptedToken = encryptionService.decrypt(posEntity.getAccessToken());
            
            // Decrypt config before using
            String decryptedConfig = encryptionService.decrypt(posEntity.getConfig());
            NhanhvnRequest request = NhanhvnRequest.buildRequest(
                    decryptedConfig,
                    decryptedToken,
                    posEntity.getCreatedAt().toInstant(ZoneOffset.UTC).getEpochSecond()
            );

            if (isInvalidRequest(request)) {
                syncHistoryRepository.save(toSyncHistory(history, (SyncErrorMessage.MISSING_CONFIG), false));
                log.error("[NhanhvnServiceImpl.syncProduct] Missing required config for posId={}", posId);
                return;
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
                    if (response.getCode().equals(NhanhvnConstants.SUCCESS_CODE)) {
                        break;
                    }
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.CONNECTION_FAILED, false));
                    log.info("[NhanhvnServiceImpl.syncProduct] No products found with paginator: {}", paginator);
                    throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_FAILED));
                }

                // product
                List<ProductEntity> pageProducts = nhanhvnMapper.convertToProductEntities(posId, response.getData(), username);
                allProducts.addAll(pageProducts);
                log.info("[NhanhvnServiceImpl.syncProduct] Fetched {} products, total so far: {}", pageProducts.size(), allProducts.size());

                // variant
                List<ProductVariantEntity> pageVariants = nhanhvnMapper.convertToVariantEntities(posId, response.getData(), username);
                allVariants.addAll(pageVariants);
                log.info("[NhanhvnServiceImpl.syncProduct] Fetched {} variants, total so far: {}", pageVariants.size(), allVariants.size());

                // xử lý next
                if (ObjectUtils.isNotEmpty(response.getPaginator()) && ObjectUtils.isNotEmpty(response.getPaginator().getNext())) {
                    paginator.setNext(response.getPaginator().getNext());
                } else {
                    break; // hết data
                }
            }
            // Lưu song song product và variant
            CompletableFuture<Void> saveProductsFuture =
                    generalPosService.saveAllAsync(allProducts, batchSize, productRepository, PosConstants.PRODUCT);

            CompletableFuture<Void> saveVariantsFuture =
                    generalPosService.saveAllAsync(allVariants, batchSize, variantRepository, PosConstants.VARIANT);

            // Chờ cả hai xong
            CompletableFuture.allOf(saveProductsFuture, saveVariantsFuture).join();

            syncHistoryRepository.save(toSyncHistory(history, null, true));

            log.info("[NhanhvnServiceImpl.syncProduct] Successfully synced {} products and {} variants from Nhanh.vn", allProducts.size(), allVariants.size());
        } catch (Exception e) {
            log.error("[NhanhvnServiceImpl.syncProduct] Failed to sync Nhanh.vn data - {}", e.getMessage(), e);
            syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.TECHNICAL_ERROR, false));
        }
    }

    //------------------------------------------------------------------------------------------

    @Override
    @Async("syncExecutor")
    public void syncOrder(String posId ) {
        SyncHistoryEntity history = SyncHistoryEntity.builder()
                .posId(posId)
                .startTime(LocalDateTime.now())
                .syncStatus(PosStatus.FAIL.name())
                .syncType(SyncType.ORDER.getValue())
                .build();
        try {
            String username = claimUtil.getUserName();
            // lấy PosEntity và validate posName
            PosEntity posEntity = generalPosService.getPos(posId);
            
            // Decrypt access token before using for API calls
            String decryptedToken = encryptionService.decrypt(posEntity.getAccessToken());
            
            // Decrypt config before using
            String decryptedConfig = encryptionService.decrypt(posEntity.getConfig());
            NhanhvnRequest request = NhanhvnRequest.buildRequest(
                    decryptedConfig,
                    decryptedToken,
                    posEntity.getCreatedAt().toInstant(ZoneOffset.UTC).getEpochSecond()
            );

            if (isInvalidRequest(request)) {
                syncHistoryRepository.save(toSyncHistory(history, (SyncErrorMessage.MISSING_CONFIG), false));
                log.error("[NhanhvnServiceImpl.syncOrder] Missing required config for posId={}", posId); // throw techial
                return;
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
                    return;
                }

                NhanhvnOrderResponse response = responseOpt.get();

                if (ObjectUtils.isEmpty(response.getData())) {
                    if (response.getCode() == NhanhvnConstants.SUCCESS_CODE) {
                        break;
                    }
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.CONNECTION_FAILED, false));
                    log.info("[NhanhvnServiceImpl.syncOrder] No order found with paginator: {}", paginator);
                    return;
                }

                // order
                List<OrderEntity> pageOrders = nhanhvnMapper.convertToOrderEntities(posId, response.getData(), username);
                allOrders.addAll(pageOrders);
                log.info("[NhanhvnServiceImpl.syncOrder] Fetched {} orders, total so far: {}", pageOrders.size(), pageOrders.size());


                List<OrderItemEntity> pageOrderItem = nhanhvnMapper.convertToOrderItemEntities(response.getData(), username);
                allOrderItems.addAll(pageOrderItem);
                log.info("[NhanhvnServiceImpl.syncOrder] Fetched {} order items, total so far: {}", pageOrderItem.size(), pageOrderItem.size());

                // xử lý next
                if (ObjectUtils.isNotEmpty(response.getPaginator()) && ObjectUtils.isNotEmpty(response.getPaginator().getNext())) {
                    paginator.setNext(response.getPaginator().getNext());
                } else {
                    break; // hết data
                }
            }

            CompletableFuture<Void> saveOrdersFuture =
                    generalPosService.saveAllAsync(allOrders, batchSize, orderRepository, PosConstants.ORDER);

            CompletableFuture<Void> saveOrderItemsFuture =
                    generalPosService.saveAllAsync(allOrderItems, batchSize, orderItemRepository, PosConstants.ORDER_ITEM);

            CompletableFuture.allOf(saveOrdersFuture, saveOrderItemsFuture).join();
            syncHistoryRepository.save(toSyncHistory(history, null, true));

            log.info("[NhanhvnServiceImpl.syncOrder] Successfully synced {} orders and {} order items from Nhanh.vn", allOrders.size(), allOrderItems.size());
        } catch (Exception e) {
            log.error("[NhanhvnServiceImpl.syncOrder] Failed to sync Nhanh.vn data order - {}", e.getMessage(), e);
            syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.TECHNICAL_ERROR, false));
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
            // Encrypt access token before storing
            String encryptedToken = encryptionService.encrypt(tokenResponse.getData().getAccessToken());
            
            PosEntity newPos = PosEntity.builder()
                .posName(PosName.NHANHVN.getValue())
                .userId(claimUtil.getUserId())
                .status(PosStatus.ACTIVE.name())
                .accessToken(encryptedToken)
                .config(encryptionService.encrypt(JsonUtils.toJson(configMap)))
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

    private boolean isInvalidRequest(NhanhvnRequest request) {
        return ObjectUtils.isEmpty(request.getAppId())
                || ObjectUtils.isEmpty(request.getBusinessId())
                || ObjectUtils.isEmpty(request.getAccessToken());
    }
}