package com.nemi.service;

import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.response.PosConnectionResponse;

import java.util.List;

public interface PosManagementService {
    String getPosName();
    String getPosStatus(String transactionId);

    String setPosStatus(String status);

    List<PosConnectionResponse> listPosConnection(String userId);

    /**
     *
     * @param {
     *     "id": "string",
     *     "apiUrl": "string",
     *     "shopId": "string"
     * }
     * @return {
     *     "status" : "PENDING",
     *     "transactionId" : "fhjsdhfjds-ksffjsk-rjewhf-sfdj" // UUID
     * }
     */
    PosConnectionResponse registerPos(PosConnectionRequest posConnectionRequest);

    /**
     * get accessCode + exchange accesstoken + sync + save db
     * @return
     */
    PosConnectionResponse connectPos();
}