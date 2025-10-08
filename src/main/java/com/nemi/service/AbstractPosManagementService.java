package com.nemi.service;

import com.nemi.enums.PosStatus;
import com.nemi.entity.PosEntity;
import com.nemi.entity.SyncHistoryEntity;
import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.model.request.ChangeStatusRequest;
import com.nemi.model.response.PosConnectionResponse;
import com.nemi.model.response.StatusResponse;
import com.nemi.repository.PosRepository;
import com.nemi.repository.SyncHistoryRepository;
import com.nemi.util.ClaimUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@RequiredArgsConstructor
public abstract class AbstractPosManagementService {
    protected final PosRepository posRepository;
    protected final ClaimUtil claimUtil;
    protected final SyncHistoryRepository syncHistoryRepository;
    protected final PosReAuthService posReAuthService;

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
                    if (posReAuthService.isAccessTokenExpired(pos)) {
                        if (!PosStatus.EXPIRED.name().equals(pos.getStatus())) {
                            pos.setStatus(PosStatus.EXPIRED.name());
                            posRepository.save(pos);
                            log.info("POS expired for userId={}, posId={}", userId, pos.getId());
                        }

                        String reAuthLink = posReAuthService.buildReAuthLink(pos);
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
}


