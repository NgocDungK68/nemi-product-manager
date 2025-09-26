package com.nemi.service;

import com.nemi.model.request.ChangeStatusRequest;
import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.response.PosConnectionResponse;
import com.nemi.model.response.StatusResponse;

import java.util.List;

public interface PosManagementService {
    String getPosName();

    /**
     * get accessCode + exchange accesstoken + sync + save db
     * @return
     */
    PosConnectionResponse connectPos(PosConnectionRequest posConnectionRequest);

    StatusResponse getPosStatus(String posId);

    PosConnectionResponse setPosStatus(ChangeStatusRequest changeStatusRequest);

    List<PosConnectionResponse> getAllPos();

}