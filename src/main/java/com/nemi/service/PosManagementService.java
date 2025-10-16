package com.nemi.service;

import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.response.PosConnectionResponse;

public interface PosManagementService {
    String getPosName();

    /**
     * get accessCode + exchange accesstoken + sync + save db
     */
    PosConnectionResponse connectPos(PosConnectionRequest posConnectionRequest);

    void syncProduct(String posId );
    void syncOrder(String posId);
}