package com.nemi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.configuration.NhanhvnConfig;
import com.nemi.constant.NhanhvnConstants;
import com.nemi.entity.PosEntity;
import com.nemi.entity.SyncHistoryEntity;
import com.nemi.enums.PosStatus;
import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.model.request.ChangeStatusRequest;
import com.nemi.model.response.PosConnectionResponse;
import com.nemi.model.response.StatusResponse;
import com.nemi.repository.PosRepository;
import com.nemi.repository.SyncHistoryRepository;
import com.nemi.util.ClaimUtil;
import io.jsonwebtoken.lang.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Slf4j
@RequiredArgsConstructor
public abstract class AbstractPosManagementService {
    protected final PosRepository posRepository;
    protected final ClaimUtil claimUtil;
    protected final SyncHistoryRepository syncHistoryRepository;
    protected final ObjectMapper objectMapper;
    protected final NhanhvnConfig  nhanhvnConfig;
    private final EncryptionService encryptionService;

    /**
     * Get POS status by ID
     */
    public StatusResponse getPosStatus(String posId) {
        SyncHistoryEntity syncHistoryEntity = syncHistoryRepository.findByPosId(posId)
                .orElseThrow(() -> new TechnicalException(AlertMessages.alert(TechnicalAlertCode.SYNC_HISTORY_NOT_FOUND)));
        return new StatusResponse(syncHistoryEntity.getSyncStatus());
    }

    /**
     * Update POS status by ID
     */
    public PosConnectionResponse setPosStatus(ChangeStatusRequest changeStatusRequest) {
        PosEntity pos = posRepository.findByIdAndUserId(changeStatusRequest.getPosId(), claimUtil.getUserId())
                .orElseThrow(() -> {
                    log.warn("POS with id={} not found, cannot update status", changeStatusRequest.getPosId());
                    return new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_FAILED));
                });
        log.info("Updating POS id={} from status={} to status={}", changeStatusRequest.getPosId(), pos.getStatus(), changeStatusRequest.getStatus());
        PosStatus posStatus = PosStatus.valueOf(changeStatusRequest.getStatus());
        pos.setStatus(posStatus.name());
        PosEntity updated = posRepository.save(pos);

        log.info("Updated POS id={} successfully", changeStatusRequest.getPosId());
        return PosConnectionResponse.toPosConnectionResponse(updated);
    }

    /**
     * List all POS connections for a user
     */
    public List<PosConnectionResponse> getAllPos() {
        String userId = claimUtil.getUserId();
        List<PosEntity> posEntities = posRepository.findByUserId(userId);
        log.debug("Found {} POS entities for userId={}", posEntities.size(), userId);

        return posEntities.stream()
                .map(pos -> {
                    // Check token hết hạn
                    if (isAccessTokenExpired(pos)) {
                        if (!PosStatus.EXPIRED.name().equals(pos.getStatus())) {
                            pos.setStatus(PosStatus.EXPIRED.name());
                            posRepository.save(pos);
                            log.info("POS expired for userId={}, posId={}", userId, pos.getId());
                        }

                        String reAuthLink = buildReAuthLink(pos);
                        return PosConnectionResponse.expired(pos, reAuthLink);
                    }

                    // Trường hợp token còn hạn, status ACTIVE
                    return PosConnectionResponse.toPosConnectionResponse(pos);
                })
                .collect(Collectors.toList());
    }

    public PosEntity getPos(String posId) {
        log.debug("posId: {}", posId);

        // Lấy PosEntity từ DB
        return posRepository.findById(posId)
                .orElseThrow(() -> {
                    log.error("Error not found posId: {}", posId);
                    return new TechnicalException(AlertMessages.alert(TechnicalAlertCode.DATA_INVALID));
                });
    }

    public <T> void saveAll(List<T> entities, int batchSize, JpaRepository<T, ?> repository, String entityName) {
        log.info("Saving {} {}s asynchronously on thread: {}",
                entities.size(), entityName, Thread.currentThread().getName());

        if (entities.isEmpty()) {
            log.info("No {} to save.", entityName);
        }

        try {
            for (int i = 0; i < entities.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, entities.size());
                List<T> batch = entities.subList(i, endIndex);

                repository.saveAll(batch);
                log.info("Saved batch {}-{} of {} {}s (Thread: {})",
                        i + 1, endIndex, entities.size(), entityName, Thread.currentThread().getName());
            }

            log.info("Successfully saved all {} {}s", entities.size(), entityName);
        } catch (Exception e) {
            log.error("Failed to save {}: {}", entityName, e.getMessage(), e);
        }
    }

    public <T> CompletableFuture<Void> saveAllAsync(List<T> entities, int batchSize, JpaRepository<T, ?> repository, String entityName) {
        log.debug("Saving {} {}s asynchronously on thread: {}", entities.size(), entityName, Thread.currentThread().getName());

        if (entities.isEmpty()) {
            log.debug("No {} to save.", entityName);
            return CompletableFuture.completedFuture(null);
        }

        // Danh sách các CompletableFuture đại diện cho từng batch
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < entities.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, entities.size());
                List<T> batch = entities.subList(i, endIndex);

                repository.saveAll(batch);
                log.debug("Saved batch {}-{} of {} {}s (Thread: {})",
                        i + 1, endIndex, entities.size(), entityName, Thread.currentThread().getName());
            }
        } catch (Exception e) {
            log.error("Failed to save {}: {}", entityName, e.getMessage(), e);
        }

        // Chờ tất cả batch hoàn tất
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> log.info("Successfully saved all {} {}s", entities.size(), entityName));
    }

    /**
     * Kiểm tra accessToken của POS đã hết hạn chưa
     */
    private boolean isAccessTokenExpired(PosEntity posEntity) {
        if (Objects.isEmpty(posEntity.getExpiredTime())) {
            return false;
        }
        return posEntity.getExpiredTime().isBefore(LocalDateTime.now());
    }

    public String buildReAuthLink(PosEntity posEntity) {
        try {
            Map<String, String> configMap = objectMapper.readValue(
                    posEntity.getConfig(),
                    new TypeReference<>() {
                    }
            );

            String appId = configMap.get(NhanhvnConstants.APP_ID);
            String businessId = configMap.get(NhanhvnConstants.BUSINESS_ID);
            if (org.springframework.util.ObjectUtils.isEmpty(appId) || org.springframework.util.ObjectUtils.isEmpty(businessId)) {
                log.warn("AppId or BusinessId is empty, AppId: {}, BusinessId: {}", appId, businessId);
                return null;
            }

            return UriComponentsBuilder.fromHttpUrl(nhanhvnConfig.getBaseUrl())
                    .pathSegment(nhanhvnConfig.getUrlOauth())
                    .queryParam(NhanhvnConstants.VERSION, nhanhvnConfig.getApiVersion())
                    .queryParam(NhanhvnConstants.APP_ID, appId)
                    .queryParam(NhanhvnConstants.BUSINESS_ID, businessId)
                    .queryParam(NhanhvnConstants.RETURN_LINK, nhanhvnConfig.getReturnLink())
                    .toUriString();
        } catch (Exception e) {
            log.error("Build Nhanh.vn reAuth link failed: {}", e.getMessage(), e);
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.JSON_PARSE_ERROR));
        }
    }
    public String generateWebhookToken(String shopId){
        String rawData =  shopId + ":" + System.currentTimeMillis();
        return Base64.getEncoder().encodeToString(rawData.getBytes());
    }

}
