package com.nemi.controller;

import com.nemi.model.request.ChangeStatusRequest;
import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.response.PosConnectionResponse;
import com.nemi.model.response.StatusResponse;
import com.nemi.service.GeneralPosService;
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
    private final GeneralPosService generalPosService;

    // POS-specific endpoints (require posName)
    @PostMapping("/{posName}/pos")
    public ResponseEntity<PosConnectionResponse> connectPos(@PathVariable String posName,
                                                            @RequestBody PosConnectionRequest posConnectionRequest) {
        PosManagementService posManagementService = posManagementFactory.getPosName(posName);
        PosConnectionResponse posConnectionResponse = posManagementService.connectPos(posConnectionRequest);
        if( posManagementService.syncData(posConnectionResponse.getId())){
            posManagementService.syncOrder(posConnectionResponse.getId());
        }
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

}