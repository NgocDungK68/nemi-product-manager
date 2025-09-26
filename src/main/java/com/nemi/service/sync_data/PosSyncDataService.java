package com.nemi.service.sync_data;

import com.nemi.entity.PosEntity;
import com.nemi.entity.ProductEntity;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PosSyncDataService {
    String getPosName();
    boolean trigger(String posId);
    void saveAllProductsSync(List<ProductEntity> products);
    PosEntity getPos(String posId);
}
