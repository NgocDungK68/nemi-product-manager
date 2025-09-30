package com.nemi.service;

import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.response.PosConnectionResponse;

public interface PosManagementService {
    String getPosName();

    /**
     * get accessCode + exchange accesstoken + sync + save db
     * @return
     */
    PosConnectionResponse connectPos(PosConnectionRequest posConnectionRequest);

    boolean syncData(String posId);

    boolean syncOrder(String posId);

}