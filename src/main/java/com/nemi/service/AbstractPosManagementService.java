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
        List<PosEntity> posEntities = posRepository.findByUserId(claimUtil.getUserId());
        log.debug("Found {} POS entities for userId={}", posEntities.size(), claimUtil.getUserId());

        return posEntities.stream()
                .map(PosConnectionResponse::toPosConnectionResponse)
                .collect(Collectors.toList());
    }
}


