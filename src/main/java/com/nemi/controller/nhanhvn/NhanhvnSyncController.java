package com.nemi.controller.nhanhvn;

import com.nemi.service.nhanhvn.NhanhvnSyncDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/nhanhvn/sync")
public class NhanhvnSyncController {
    private final NhanhvnSyncDataService nhanhvnSyncDataService;

    @PostMapping("/products")
    public Mono<ResponseEntity<String>> syncProducts() {
        log.info("Received request to sync products from Nhanh.vn");

        return nhanhvnSyncDataService.triggerSyncNhanhvnData()
                .map(result -> {
                    log.info("Sync completed successfully: {}", result);
                    return ResponseEntity.ok(result);
                })
                .onErrorResume(error -> {
                    log.error("Sync failed: {}", error.getMessage(), error);
                    return Mono.just(ResponseEntity.internalServerError()
                            .body("Sync failed: " + error.getMessage()));
                });
    }
}