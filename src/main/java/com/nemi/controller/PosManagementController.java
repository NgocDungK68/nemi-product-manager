package com.nemi.controller;

import com.nemi.model.request.ChangeStatusRequest;
import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.response.PosConnectionResponse;
import com.nemi.model.response.StatusResponse;
import com.nemi.service.PosManagementService;
import com.nemi.service.factory.PosManagementFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/client-api/v1")
@RequiredArgsConstructor
public class PosManagementController {
    private final PosManagementFactory posManagementFactory;

    @PostMapping("/{posName}/pos")
    public ResponseEntity<PosConnectionResponse> connectPos(@PathVariable String posName,
                                                            @RequestBody PosConnectionRequest posConnectionRequest) {
        PosManagementService posManagementService = posManagementFactory.getPosName(posName);
        PosConnectionResponse posConnectionResponse = posManagementService.connectPos(posConnectionRequest);
        posManagementService.syncData(posConnectionResponse.getId());
        return ResponseEntity.ok(posConnectionResponse);
    }

    @PutMapping("/{posName}/pos")
    public ResponseEntity<PosConnectionResponse> changeStatusPos(@PathVariable String posName,
                                                                 @RequestBody ChangeStatusRequest changeStatusRequest) {
        PosManagementService posManagementService = posManagementFactory.getPosName(posName);
        PosConnectionResponse posConnectionResponse =  posManagementService.setPosStatus(changeStatusRequest);
        return ResponseEntity.ok(posConnectionResponse);
    }

    @GetMapping("/{posName}/pos/{posId}/status")
    public ResponseEntity<StatusResponse> getStatusPos(
            @PathVariable String posName,
            @PathVariable String posId) {
        PosManagementService posManagementService = posManagementFactory.getPosName(posName);
        StatusResponse statusResponse =  posManagementService.getPosStatus(posId);
        return ResponseEntity.ok(statusResponse);
    }

    @GetMapping("/{posName}/pos")
    public ResponseEntity<List<PosConnectionResponse>> listAllPos(  @PathVariable String posName) {
        PosManagementService posManagementService = posManagementFactory.getPosName(posName);
        List<PosConnectionResponse> listPosConnection = posManagementService.getAllPos();
        return ResponseEntity.ok(listPosConnection);
    }

}