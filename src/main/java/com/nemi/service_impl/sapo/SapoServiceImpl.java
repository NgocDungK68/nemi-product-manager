package com.nemi.service_impl.sapo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.client.SapoClient;
import com.nemi.configuration.SapoConfig;
import com.nemi.constant.PosConstants;
import com.nemi.constant.SapoConstants;
import com.nemi.entity.*;
import com.nemi.enums.PosName;
import com.nemi.enums.PosStatus;
import com.nemi.enums.SyncErrorMessage;
import com.nemi.enums.SyncType;
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
import com.nemi.mapper.SapoMapper;
import com.nemi.service.GeneralPosService;
import com.nemi.service.PosManagementService;
import com.nemi.service.EncryptionService;
import com.nemi.util.ClaimUtil;
import com.nemi.util.JsonUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class SapoServiceImpl implements PosManagementService {


    private final ClaimUtil claimUtil;
    private final SapoClient sapoClient;
    private final ProductRepository productRepository;
    private final PosRepository posRepository;
    private final ProductVariantRepository variantRepository;
    private final SyncHistoryRepository syncHistoryRepository;
    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final SapoConfig sapoConfig;
    private final GeneralPosService generalPosService;
    private final EncryptionService tokenEncryptionService;
    private final SapoMapper sapoMapper;

    private int pageStartNumber;
    private int productLimit;
    private int batchSize;

    @PostConstruct
    public void init() {
        pageStartNumber = sapoConfig.getSync().getPageStart();
        productLimit = sapoConfig.getSync().getProductLimit();
        batchSize = sapoConfig.getSync().getBatchSize();
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

            // Delete old webhooks (posId khác) trước khi đăng ký mới
            sapoClient.deleteWebhook(
                    posConnectionRequest.getStoreName(),
                    tokenResponse.getAccessToken(),
                    newPos.getId()
            );

            // Register webhooks for current POS
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
    public void syncProduct(String posId, Boolean isSyncAll) {
        SyncHistoryEntity history = SyncHistoryEntity.builder()
                .posId(posId)
                .startTime(LocalDateTime.now())
                .syncStatus(PosStatus.FAIL.name())
                .syncType(SyncType.PRODUCT.getValue())
                .build();
        try {
            String username = claimUtil.getUserName();
            //B1 : Lay posentity va validate posName
            PosEntity posEntity = generalPosService.getPos(posId);
            // B2: parse config
            SapoRequest request = buildRequest(posEntity);

            if (isInvalidRequest(request)) {
                syncHistoryRepository.save(toSyncHistory(history, (SyncErrorMessage.MISSING_CONFIG), false));
                log.error("[NhanhvnServiceImpl.syncProduct] Missing required config for posId={}", posId);
                return;
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

                // Check if API response is present
                if (responseOpt.isEmpty()) {
                    log.error("[SapoServiceImpl.syncProduct] API returned empty response");
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.TECHNICAL_ERROR, false));
                    return;
                }

                // Extract products from response
                SapoProductResponse response = responseOpt.get();
                List<SapoProductResponse.Product> products = response.getProducts();

                // Đây là khi đã sync hết tất cả products (end of pagination)
                if (ObjectUtils.isEmpty(products)) {
                    log.info("[SapoServiceImpl.syncProduct] Reached end of pagination (page={})", paginator.getPage());
                    break;
                }

                // Convert products and variants using SapoMapper
                for (SapoProductResponse.Product sapoProduct : products) {
                    ProductEntity productEntity = sapoMapper.convertToProductEntity(posId, sapoProduct, username);
                    allProducts.add(productEntity);

                    // Convert variants
                    if (ObjectUtils.isNotEmpty(sapoProduct.getVariants())) {
                        List<ProductVariantEntity> variants = sapoMapper.convertToVariantEntities(posId, productEntity.getProductId(), sapoProduct.getVariants(), username);
                        allVariants.addAll(variants);
                    }
                }
                log.info("Fetched {} products and {} variants", products.size(), allVariants.size());

                // Continue pagination if fetched full page
                if (products.size() >= productLimit) {
                    pageStartNumber++;
                    paginator.setPage(pageStartNumber);
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

            log.info("Successfully synced {} products and {} variants from Sapo", allProducts.size(), allVariants.size());
        } catch (Exception e) {
            log.error("Failed to sync Sapo data - {}", e.getMessage(), e);
            syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.TECHNICAL_ERROR, false));
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



    @Override
    public void syncOrder(String posId, Boolean isSyncAll) {
        SyncHistoryEntity history = SyncHistoryEntity.builder()
                .posId(posId)
                .startTime(LocalDateTime.now())
                .syncStatus(PosStatus.FAIL.name())
                .syncType(SyncType.ORDER.getValue())
                .build();
        try {
            PosEntity posEntity = generalPosService.getPos(posId);

            // Decrypt config before using
            String decryptedConfig = tokenEncryptionService.decrypt(posEntity.getConfig());
            Map<String, String> configMap = objectMapper.readValue(
                    decryptedConfig, new TypeReference<>() {
                    });

            String clientId = configMap.get(SapoConstants.CLIENT_ID);
            String clientSecret = configMap.get(SapoConstants.CLIENT_SECRET);
            String storeName = configMap.get(SapoConstants.STORE_NAME);
            
            // Decrypt access token when using for API calls
            String accessToken = tokenEncryptionService.decrypt(posEntity.getAccessToken());

            if (StringUtils.isEmpty(clientId) || StringUtils.isEmpty(clientSecret) || StringUtils.isEmpty(storeName) || StringUtils.isEmpty(accessToken)) {
                syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.MISSING_CONFIG, false));
                log.error("Missing required config for posId={}", posId);
                return;
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

                String username = claimUtil.getUserName();
                List<OrderEntity> pageOrders = sapoMapper.convertToOrderEntities(posId, response.getOrders(), username);
                allOrders.addAll(pageOrders);

                List<OrderItemEntity> pageOrderItems = sapoMapper.convertToOrderItemEntities(response.getOrders(), username);
                allOrderItems.addAll(pageOrderItems);

                // Continue pagination: increment page; stop if returned less than limit
                if (response.getOrders().size() >= paginator.getLimit()) {
                    paginator.setPage(paginator.getPage() + 1);
                } else {
                    break;
                }
            }

            CompletableFuture<Void> saveOrdersFuture =
                    generalPosService.saveAllAsync(allOrders, batchSize, orderRepository, PosConstants.ORDER);

            CompletableFuture<Void> saveOrderItemsFuture =
                    generalPosService.saveAllAsync(allOrderItems, batchSize, orderItemRepository, PosConstants.ORDER_ITEM);

            CompletableFuture.allOf(saveOrdersFuture, saveOrderItemsFuture).join();
            syncHistoryRepository.save(toSyncHistory(history, null, true));

            log.info("Successfully synced {} orders from Pancake", allOrders.size());
            log.info("Successfully synced {} order items from Pancake", allOrderItems.size());
        } catch (Exception e) {
            log.error("Failed to sync Pancake orders - {}", e.getMessage(), e);
            syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.ORDER_TECHNICAL_ERROR, false));
        }
    }

    private PosEntity createNewPos(SapoAccessTokenResponse tokenResponse, Map<String, String> configMap) {
        // Encrypt access token before storing in database
        String encryptedAccessToken = tokenEncryptionService.encrypt(tokenResponse.getAccessToken());
        
        PosEntity newPos = PosEntity.builder()
                .posName(PosName.SAPO.getValue())
                .userId(claimUtil.getUserId())
                .status(PosStatus.ACTIVE.name())
                .accessToken(encryptedAccessToken)
                .config(tokenEncryptionService.encrypt(JsonUtils.toJson(configMap)))
                .expiredTime(null)
                .companyId(String.valueOf(claimUtil.getCompanyId()))
                .createdBy(claimUtil.getUserName())
                .build();

        return posRepository.save(newPos);
    }

    public SapoRequest buildRequest(PosEntity posEntity) {
        try {
            //parse config
            // Decrypt config before using
            String decryptedConfig = tokenEncryptionService.decrypt(posEntity.getConfig());
            Map<String, String> configMap = objectMapper.readValue(
                    decryptedConfig,
                    new TypeReference<>() {
                    }
            );

            String clientId = configMap.get(SapoConstants.CLIENT_ID);
            String clientSecret = configMap.get(SapoConstants.CLIENT_SECRET);
            String storeName = configMap.get(SapoConstants.STORE_NAME);
            
            // Decrypt access token when using for API calls
            String decryptedAccessToken = tokenEncryptionService.decrypt(posEntity.getAccessToken());

            return SapoRequest.builder()
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .storeName(storeName)
                    .accessToken(decryptedAccessToken)
                    .build();
        } catch (Exception e) {
            log.error("Failed request SapoRequest - {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.JSON_PARSE_ERROR));
        }
    }

    private boolean isInvalidRequest(SapoRequest request) {
        return ObjectUtils.isEmpty(request.getClientId())
                || ObjectUtils.isEmpty(request.getClientSecret())
                || ObjectUtils.isEmpty(request.getAccessToken());
    }
}
