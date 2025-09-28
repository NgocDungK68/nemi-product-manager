package com.nemi.service_impl.pancake;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.client.NhanhvnClient;
import com.nemi.client.PancakeClient;
import com.nemi.constant.enums.PosName;
import com.nemi.constant.enums.PosStatus;
import com.nemi.constant.enums.SyncErrorMessage;
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
import com.nemi.model.response.nhanhvn.NhanhvnProductResponse;
import com.nemi.model.response.pancake.PancakeProductResponse;
import com.nemi.repository.PosRepository;
import com.nemi.repository.ProductRepository;
import com.nemi.repository.SyncHistoryRepository;
import com.nemi.service.AbstractPosManagementService;
import com.nemi.service.PosManagementService;
import com.nemi.util.ClaimUtil;
import com.nemi.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PancakeServiceImpl extends AbstractPosManagementService implements PosManagementService {
    private final ClaimUtil claimUtil;
    private final PancakeClient pancakeClient;
    private final ObjectMapper objectMapper;
    private final ProductRepository productRepository;
    private final SyncHistoryRepository syncHistoryRepository;

    public PancakeServiceImpl(PosRepository posRepository, ClaimUtil claimUtil,
                              PancakeClient pancakeClient, ObjectMapper objectMapper,
                              ProductRepository productRepository, SyncHistoryRepository syncHistoryRepository) {
        super(posRepository, claimUtil);
        this.claimUtil = claimUtil;
        this.pancakeClient = pancakeClient;
        this.objectMapper = objectMapper;
        this.productRepository = productRepository;
        this.syncHistoryRepository = syncHistoryRepository;

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
                return false;
            }

            List<ProductEntity> allProducts = new ArrayList<>();
//--------------------------------------------------------------------------------------
            // chỉ set size cho lần đầu
            int pageSize = 150;
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
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.TECHNICAL_ERROR, false));
                    log.error("Missing required config for posId={}", posId);
                    return false;
                }

                PancakeProductResponse response = responseOpt.get();

                if (!response.isSuccess()) {
                    syncHistoryRepository.save(toSyncHistory(history, SyncErrorMessage.TECHNICAL_ERROR, false));
                    log.error("Missing required config for posId={}", posId);
                    return false;
                }

                if (response.getData() == null || response.getData().isEmpty()) {

                    log.info("No products found with page number: {}",pageNumber);
                    break;
                } else {
                    request.setPageNumber(request.getPageNumber()+1);
                }

                List<ProductEntity> pageProducts = convertToProductEntities(posId, response.getData());
                allProducts.addAll(pageProducts);

                log.info("Fetched {} products, total so far: {}", pageProducts.size(), allProducts.size());

            }


            syncHistoryRepository.save(toSyncHistory(history, null, true));
            saveAllProductsSync(allProducts);

            log.info("Successfully synced {} products from Nhanh.vn", allProducts.size());
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
        if(apiProducts.getIsLocked()) {
            product.setStatus(PosStatus.INACTIVE.name());
        } else {
            product.setStatus(PosStatus.ACTIVE.name());
        }

        return product;
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

