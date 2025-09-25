package com.nemi.client;

import com.nemi.constant.enums.PosStatus;
import com.nemi.entity.PosEntity;
import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.response.PosConnectionResponse;
import com.nemi.model.response.StatusResponse;
import com.nemi.repository.PosRepository;
import com.nemi.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@RequiredArgsConstructor
public abstract class AbstractPosManagementService {

    protected final PosRepository posRepository;


    /**
     * Get POS status by ID
     */
    public StatusResponse getPosStatus(String posId) {

        PosEntity pos = posRepository.findById(posId)
                .orElseThrow(() -> {
                    log.warn("POS with id={} not found, cannot get status", posId);
                    return new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_FAILED));
                });

        return new StatusResponse(pos.getStatus());
    }

    /**
     * Update POS status by ID
     */
    public PosConnectionResponse setPosStatus(String posId, String status) {
        PosEntity pos = posRepository.findById(posId)
                .orElseThrow(() -> {
                    log.warn("POS with id={} not found, cannot update status", posId);
                    return new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_CONNECTION_FAILED));
                });

        log.info("Updating POS id={} from status={} to status={}", posId, pos.getStatus(), status);
        PosStatus posStatus = PosStatus.valueOf(status); // throw ra illgeaargumentExcetion ? can thay the bang excetion cu the khong

        pos.setStatus(posStatus.name());
        PosEntity updated = posRepository.save(pos);

        log.info("Updated POS id={} successfully", posId);
        return PosConnectionResponse.toPosConnectionResponse(updated);
    }

    /**
     * List all POS connections for a user
     */
    public List<PosConnectionResponse> listPosConnection(String userId) {
        List<PosEntity> posEntities = posRepository.findByUserId(userId);

        log.debug("Found {} POS entities for userId={}", posEntities.size(), userId);

        return posEntities.stream()
                .map(PosConnectionResponse::toPosConnectionResponse)
                .collect(Collectors.toList());
    }
    /**
     * register all POS connections for a user
     */
    public PosConnectionResponse registerPos(PosConnectionRequest posConnectionRequest) {
        PosEntity pos = JsonUtils.map(posConnectionRequest,PosEntity.class);
        posRepository.save(pos);
        return JsonUtils.map(pos,PosConnectionResponse.class);
    }
}


