package com.nemi.service_impl.nhanhvn;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.client.NhanhvnClient;
import com.nemi.constant.enums.PosName;
import com.nemi.constant.enums.PosStatus;
import com.nemi.constant.enums.SyncErrorMessage;
import com.nemi.constant.enums.WeightUnit;
import com.nemi.entity.PosEntity;
import com.nemi.entity.ProductEntity;
import com.nemi.entity.ProductVariantEntity;
import com.nemi.entity.SyncHistoryEntity;
import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.request.nhanhvn.NhanhvnRequest;
import com.nemi.model.response.PosConnectionResponse;
import com.nemi.model.response.nhanhvn.NhanhvnAccessTokenResponse;
import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;
import com.nemi.repository.PosRepository;
import com.nemi.repository.ProductRepository;
import com.nemi.repository.ProductVariantRepository;
import com.nemi.repository.SyncHistoryRepository;
import com.nemi.service.PosManagementService;
import com.nemi.util.ClaimUtil;
import com.nemi.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @Override
    public String getPosName() {
        return PosName.NHANHVN.getValue();
    }

    @Override
    public PosConnectionResponse connectPos(PosConnectionRequest posConnectionRequest) {
        try {

            String userId = claimUtil.getUserId();

            Map<String, String> configMap = new HashMap<>();
            configMap.put("secretId", posConnectionRequest.getAppSecret());
            configMap.put("appId", posConnectionRequest.getAppId());
            configMap.put("businessId", posConnectionRequest.getBusinessId());

            NhanhvnAccessTokenResponse tokenResponse = nhanhvnClient.getAccessToken(posConnectionRequest);

            if (tokenResponse.getData() == null || tokenResponse.getData().getAccessToken() == null) {
                log.error("Nhanhvn response is null, stop persist to db {}", tokenResponse);
                throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_FAILED));
            }

            LocalDateTime expiredTime = LocalDateTime.now().plusYears(1);
            PosEntity posEntityBuilder = PosEntity.builder()
                    .posName(PosName.NHANHVN.getValue())
                    .userId(userId)
                    .status(PosStatus.ACTIVE.name())
                    .accessToken(tokenResponse.getData().getAccessToken())
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

            String appId = configMap.get("appId");
            String businessId = configMap.get("businessId");
            String accessToken = posEntity.getAccessToken();

            if (appId == null || businessId == null || accessToken == null) {
                syncHistoryRepository.save(toSyncHistory(history, (SyncErrorMessage.MISSING_CONFIG), false));
                log.error("Missing required config for posId={}", posId); // throw techial
                return false;
            }

            List<ProductEntity> allProducts = new ArrayList<>();
            List<ProductVariantEntity> allVariants = new ArrayList<>();

            // set size mỗi page
            Map<String, Object> paginator = new HashMap<>();
            paginator.put("size", 50);

            NhanhvnRequest request = NhanhvnRequest.builder()
                    .appId(appId)
                    .businessId(businessId)
                    .accessToken(accessToken)
                    .paginator(paginator)
                    .build();

            while (true) {
                Optional<NhanhvnProductResponse> responseOpt = nhanhvnClient.getProducts(request);

                if (responseOpt.isEmpty()) {
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.TECHNICAL_ERROR, false));
                    log.error("Missing required config for posId={}", posId);
                    log.error("Failed to fetch products with paginator: {}", paginator);
                    return false;
                }

                NhanhvnProductResponse response = responseOpt.get();

                if (response.getData() == null || response.getData().isEmpty()) {
                    if (response.getCode() == 1) {
                        syncHistoryRepository.save(toSyncHistory(history, null, true));
                        break;
                    }
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.CONNECTION_FAILED, false));
                    log.info("No products found with paginator: {}", paginator);
                    return false;
                }

                // product
                List<ProductEntity> pageProducts = convertToProductEntities(posId, response.getData());
                allProducts.addAll(pageProducts);
                log.info("Fetched {} products, total so far: {}", pageProducts.size(), allProducts.size());

                // variant
                List<ProductVariantEntity> pageVariants = convertToVariantEntities(response.getData());
                allVariants.addAll(pageVariants);
                log.info("Fetched {} variants, total so far: {}", pageVariants.size(), allVariants.size());

                // xử lý next
                if (response.getPaginator() != null && response.getPaginator().getNext() != null) {
                    paginator.put("next", response.getPaginator().getNext());
                } else {
                    break; // hết data
                }
            }
            syncHistoryRepository.save(toSyncHistory(history, null, true));
            saveAllProductsSync(allProducts);
            saveAllVariantsSync(allVariants);

            log.info("Successfully synced {} products from Nhanh.vn", allProducts.size());
            log.info("Successfully synced {} variants from Nhanh.vn", allVariants.size());
            return true;

        } catch (Exception e) {
            log.error("Failed to sync Nhanh.vn data - {}", e.getMessage(), e);
            syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.TECHNICAL_ERROR, false));
            return false;
        }
    }

    public void saveAllProductsSync(List<ProductEntity> products) {
        log.info("Saving {} Nhanh.vn products synchronously", products.size());

        if (products.isEmpty()) {
            log.info("No products to save.");
            return;
        }

        try {
            int batchSize = 50;
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
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.DATA_PERSISTENCE_ERROR));
        }
    }

    public void saveAllVariantsSync(List<ProductVariantEntity> variants) {
        log.info("Saving {} Nhanh.vn variants synchronously", variants.size());

        if (variants.isEmpty()) {
            log.info("No variants to save.");
            return;
        }

        try {
            int batchSize = 50;
            for (int i = 0; i < variants.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, variants.size());
                List<ProductVariantEntity> batch = variants.subList(i, endIndex);

                productVariantRepository.saveAll(batch);
                log.info("Saved batch {}-{} of {} variants",
                        i + 1, endIndex, variants.size());
            }

            log.info("Successfully saved all {} Nhanh.vn variants", variants.size());
        } catch (Exception e) {
            log.error("Failed to save Nhanh.vn variants synchronously: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.DATA_PERSISTENCE_ERROR));
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

    private List<ProductEntity> convertToProductEntities(String posId, List<NhanhvnProductResponse.ProductData> apiProducts) {
        return apiProducts.stream()
                .map(apiProduct -> convertToProductEntity(posId, apiProduct))
                .filter(Objects::nonNull)
                .toList();
    }

    public ProductEntity convertToProductEntity(String posId, NhanhvnProductResponse.ProductData apiProduct) {
        if (apiProduct.getParentId() != -2) return null;
        return ProductEntity.builder()
                .posId(posId)
                .productId(String.valueOf(apiProduct.getId()))
                .code(apiProduct.getCode())
                .name(apiProduct.getName())
                .status(apiProduct.getStatus().toString())
                .build();
    }

    private List<ProductVariantEntity> convertToVariantEntities(List<NhanhvnProductResponse.ProductData> apiProducts) {
        return apiProducts.stream()
                .map(this::convertToVariantEntity)
                .filter(Objects::nonNull)
                .toList();
    }

    public ProductVariantEntity convertToVariantEntity(NhanhvnProductResponse.ProductData apiProduct) {
        if (apiProduct.getParentId() == -2) return null;
        return ProductVariantEntity.builder()
                .variantId(String.valueOf(apiProduct.getId()))
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

    private SyncHistoryEntity toSyncHistory(SyncHistoryEntity syncHistoryEntity, SyncErrorMessage syncErrorMessage, Boolean isSyncSuccess) {
        if (isSyncSuccess == false) {
            syncHistoryEntity.setEndTime(LocalDateTime.now());
            syncHistoryEntity.setErrorMessage(syncErrorMessage.getMessage());
            return syncHistoryEntity;
        }
        syncHistoryEntity.setSyncStatus(PosStatus.SUCCESS.name());
        syncHistoryEntity.setEndTime(LocalDateTime.now());
        return syncHistoryEntity;
    }

}

