package com.nemi.controller;

import com.nemi.client.PosManagementService;
import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.response.PosConnectionResponse;
import com.nemi.model.response.StatusResponse;
import com.nemi.service.factory.PosManagementFactory;
import com.nemi.service.factory.PosSyncDataFactory;
import com.nemi.service.sync_data.PosSyncDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final PosSyncDataFactory posSyncDataFactory;

    @PostMapping("/{posName}/pos")
    public ResponseEntity<PosConnectionResponse> connectPos(@PathVariable String posName,
                                                            @RequestBody PosConnectionRequest posConnectionRequest) {
        PosManagementService posManagementService = posManagementFactory.getPosName(posName);
        PosConnectionResponse posConnectionResponse = posManagementService.connectPos(posConnectionRequest);

        PosSyncDataService posSyncDataService = posSyncDataFactory.getPosName(posName);
        posSyncDataService.trigger(posConnectionResponse.getId());
        return ResponseEntity.ok(posConnectionResponse);
    }

    @PatchMapping("/{posName}/pos/{pos-id}") // cap nhat trang thai pos trong db
    public ResponseEntity<PosConnectionResponse> changeStatusPos(@PathVariable String posName, @PathVariable(name = "pos-id") String posId,
                                                                 @RequestParam String status) {
        PosManagementService posManagementService = posManagementFactory.getPosName(posName);
        PosConnectionResponse posConnectionResponse =  posManagementService.setPosStatus(posId, status);
        return ResponseEntity.ok(posConnectionResponse);
    }

    @GetMapping("/{posName}/pos/{pos-id}/auth/status") // giong getlist chi cha ra status
    public ResponseEntity<StatusResponse> getStatusPos(@PathVariable String posName, @PathVariable(name = "pos-id") String posId) {
        PosManagementService posManagementService = posManagementFactory.getPosName(posName);
        StatusResponse statusResponse =  posManagementService.getPosStatus(posId);

        return ResponseEntity.ok(statusResponse);

    }

    @GetMapping("/{posName}/pos/{userId}") // giong getlist chi cha ra status
    public ResponseEntity<List<PosConnectionResponse>> listAllPos(@PathVariable String posName, @PathVariable String userId) {

        PosManagementService posManagementService = posManagementFactory.getPosName(posName);
        List<PosConnectionResponse> listPosConnection = posManagementService.listPosConnection(userId);
        return ResponseEntity.ok(listPosConnection);

    }

    @GetMapping("/{posName}/pos/register") // giong getlist chi cha ra status
    public ResponseEntity<PosConnectionResponse> registerPos(@PathVariable String posName, @PathVariable(name = "pos-id") String posId,
                                                             @RequestBody PosConnectionRequest posConnectionRequest) {

        PosManagementService posManagementService = posManagementFactory.getPosName(posName);
        PosConnectionResponse pos = posManagementService.registerPos(posConnectionRequest);
        return ResponseEntity.ok(pos);
    }

}


// nhanhvn
// fe code-> connectPos(luu pos db voi status pending) -> authPOs(set accesToken va status thanh active)