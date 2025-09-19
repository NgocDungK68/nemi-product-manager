package com.nemi.service.pancake;

import com.nemi.entity.ProductEntity;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PancakeSyncDataService {
    Mono<String> triggerSyncPancakeData(String shopId);
    void saveAllPancakeProducts(List<ProductEntity> products);
    void saveAllPancakeProductsSync(List<ProductEntity> products);
}
