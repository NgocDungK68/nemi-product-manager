package com.nemi.controller;

import com.nemi.model.response.PosConnectionResponse;
import com.nemi.service.PosManagementService;
import com.nemi.service.factory.PosManagementFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/client-api/v1")
public class PosManagementController  {
    private PosManagementFactory posManagementFactory;
    private PosManagementService posManagementService;

//    @PostMapping("/{posName}/pos") // save ban ghi vao db pos(status pending)
//    public ResponseEntity<PosConnectionResponse> connectPos(@PathVariable String posName) {
//        PosManagementService posManagementService = posManagementFactory.getPosName(posName);
//
//    }
    // tuong tu
//    @GetMapping("/pos") //get all pos theo db(k quan trong pos type)
//    public ResponseEntity<PosConnectionResponse> listPos() {
//        posManagementService.listPosConnection();
//        return null;
//    }

    @PatchMapping("/{posName}/pos/{pos-id}") // cap nhat trang thai pos trong db
    public ResponseEntity<PosConnectionResponse> changeStatusPos(@PathVariable(name = "pos-id") String posId) {
        return null;
    }

    @GetMapping("/{posName}/pos/{transactionId}/auth/status") // giong getlist chi cha ra status
    public ResponseEntity<PosConnectionResponse> getStatusPos(@PathVariable String transactionId) {
        return null;
    }
}



// nhanhvn
// fe code-> connectPos(luu pos db voi status pending) -> authPOs(set accesToken va status thanh active)