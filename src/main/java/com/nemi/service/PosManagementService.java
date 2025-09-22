package com.nemi.service;

import com.nemi.model.PosConnection;
import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.response.PosConnectionResponse;

import java.util.List;

public interface PosManagementService {

    String getPosName();
    String getPosStatus(String transactionId);

    String setPosStatus(String status);

    List<PosConnectionResponse> listPosConnection(String userId);
    PosConnectionResponse connectPos(PosConnectionRequest posConnectionRequest);
}
