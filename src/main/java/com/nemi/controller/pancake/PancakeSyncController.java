//package com.nemi.controller.pancake;
//
//import com.nemi.service.pancake.PancakeSyncDataService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import reactor.core.publisher.Mono;
//
//@Slf4j
//@RestController
//@RequestMapping("/api/v1/pancake/sync")
//public class PancakeSyncController {
//
//    @Autowired
//    private PancakeSyncDataService pancakeSyncDataService;
//
//    /**
//     * Trigger sync all products from Pancake POS
//     * Example: POST /api/v1/pancake/sync/products/1720119150
//     */
//    @PostMapping("/products/{shopId}")
//    public Mono<ResponseEntity<String>> syncProducts(@PathVariable String shopId) {
//        log.info("Received request to sync products for shopId: {}", shopId);
//
//        return pancakeSyncDataService.triggerSyncPancakeData(shopId)
//                .map(result -> {
//                    log.info("Sync completed successfully: {}", result);
//                    return ResponseEntity.ok(result);
//                })
//                .onErrorResume(error -> {
//                    log.error("Sync failed: {}", error.getMessage(), error);
//                    return Mono.just(ResponseEntity.internalServerError()
//                            .body("Sync failed: " + error.getMessage()));
//                });
//    }
//
//    /**
//     * Health check endpoint
//     */
//    @GetMapping("/health")
//    public ResponseEntity<String> health() {
//        return ResponseEntity.ok("Pancake Sync Service is running");
//    }
//}
