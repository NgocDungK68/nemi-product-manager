package com.nemi.controller;

import com.nemi.entity.PosEntity;
import com.nemi.enums.PosStatus;
import com.nemi.enums.Status;
import com.nemi.exception.TechnicalAlertCode;
import com.nemi.exception.TechnicalException;
import com.nemi.exception.pojo.AlertMessages;
import com.nemi.model.request.ChangeStatusRequest;
import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.response.PosConnectionResponse;
import com.nemi.model.response.StatusResponse;
import com.nemi.repository.PosRepository;
import com.nemi.service.GeneralPosService;
import com.nemi.service.PosManagementService;
import com.nemi.service.factory.PosManagementFactory;
import com.nemi.util.ClaimUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/client-api/v1")
@RequiredArgsConstructor
public class PosManagementController {
    private final PosManagementFactory posManagementFactory;
    private final GeneralPosService generalPosService;
    private final PosRepository posRepository;
    private final ClaimUtil claimUtil;

    // POS-specific endpoints (require posName)
    @PostMapping("/{posName}/pos")
    public ResponseEntity<PosConnectionResponse> connectPos(@PathVariable String posName,
                                                            @RequestBody PosConnectionRequest posConnectionRequest) {
        PosEntity posEntity = posRepository.findByUserIdAndPosName(claimUtil.getUserId(), posName);
        if (ObjectUtils.isNotEmpty(posEntity) && Status.ACTIVE.getValue().equals(posEntity.getStatus())) {
            throw new TechnicalException(AlertMessages.alert(TechnicalAlertCode.POS_ALREADY_CONNECTED));
        }

        PosManagementService posManagementService = posManagementFactory.getPosName(posName);
        PosConnectionResponse posConnectionResponse = posManagementService.connectPos(posConnectionRequest);

        posManagementService.syncProduct(posConnectionResponse.getId(), posConnectionResponse.getDepartmentId());

        posManagementService.syncOrder(posConnectionResponse.getId(), posConnectionResponse.getDepartmentId());

        return ResponseEntity.ok(posConnectionResponse);
    }

    // Common endpoints (no posName needed - cleaner API)
    @GetMapping("/pos")
    public ResponseEntity<List<PosConnectionResponse>> listAllPos() {

        List<PosConnectionResponse> listPosConnection = generalPosService.getAllPos();
        return ResponseEntity.ok(listPosConnection);
    }

    @GetMapping("/pos/{posId}/status")
    public ResponseEntity<StatusResponse> getStatusPos(@PathVariable String posId) {
         StatusResponse statusResponse = generalPosService.getPosStatus(posId);
        return ResponseEntity.ok(statusResponse);
    }

    @PutMapping("/pos")
    public ResponseEntity<PosConnectionResponse> changeStatusPos(@RequestBody ChangeStatusRequest changeStatusRequest) {
        PosConnectionResponse posConnectionResponse = generalPosService.setPosStatus(changeStatusRequest);
        return ResponseEntity.ok(posConnectionResponse);
    }
    // test pancake

    @PostMapping("/pos/{posId}/sync")
    public ResponseEntity<PosConnectionResponse> manualSync(@PathVariable String posId, @RequestParam(defaultValue = "false") boolean isSyncAll) {
        PosEntity posEntity = generalPosService.getPos(posId);
        PosManagementService posManagementService = posManagementFactory.getPosName(posEntity.getPosName());
        posManagementService.syncProduct(posId, posEntity.getDepartmentId());
        posManagementService.syncOrder(posId, posEntity.getDepartmentId());
        return ResponseEntity.ok(PosConnectionResponse.toPosConnectionResponse(posEntity));
    }

    public void updateStatusIfProcessing(String posId, boolean isSuccess) {
        PosEntity posEntity = posRepository.findById(posId).orElseThrow();

        if (PosStatus.PROCESSING.name().equals(posEntity.getStatus())) {
            posEntity.setStatus(isSuccess ? PosStatus.ACTIVE.name() : PosStatus.FAIL.name());
            posRepository.save(posEntity);
        } else if (!isSuccess) {
            // Nếu cái trước đã ghi FAILURE thì không cần ghi nữa
            if (!PosStatus.FAIL.name().equals(posEntity.getStatus())) {
                posEntity.setStatus(PosStatus.FAIL.name());
                posRepository.save(posEntity);
            }
        }
    }

}