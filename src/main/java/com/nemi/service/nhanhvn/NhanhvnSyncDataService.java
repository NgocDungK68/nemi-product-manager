package com.nemi.service.nhanhvn;

import reactor.core.publisher.Mono;

public interface NhanhvnSyncDataService {
    Mono<String> triggerSyncNhanhvnData();
}