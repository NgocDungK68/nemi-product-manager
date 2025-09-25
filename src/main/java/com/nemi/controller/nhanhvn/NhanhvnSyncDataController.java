//package com.nemi.controller.nhanhvn;
//
//import com.nemi.client.PosManagementService;
//import com.nemi.model.response.PosConnectionResponse;
//import com.nemi.service.sync_data.PosSyncDataService;
//import com.nemi.service_impl.nhanhvn.NhanhvnSyncDataServiceImpl;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/client-api/v1/sync_data")
//@RequiredArgsConstructor
//public class NhanhvnSyncDataController {
//    private final NhanhvnSyncDataServiceImpl nhanhvnSyncDataServiceImpl;
//
//    @PostMapping()
//    public ResponseEntity<String> exchangeAccessToken() {
//        PosManagementService posManagementService = posManagementFactory.getPosName(posName);
//        PosConnectionResponse posConnectionResponse = posManagementService.connectPos(posConnectionRequest);
//
//        PosSyncDataService posSyncDataService = posSyncDataFactory.getPosName(posName);
//        posSyncDataService.trigger(posConnectionResponse.getId());
//        return ResponseEntity.ok("OK");
//    }
//}
