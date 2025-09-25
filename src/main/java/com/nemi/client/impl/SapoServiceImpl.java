package com.nemi.client.impl;

import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.response.PosConnectionResponse;
import com.nemi.client.PosManagementService;
import com.nemi.model.response.StatusResponse;

import java.util.List;

public class SapoServiceImpl implements PosManagementService {

    @Override
    public String getPosName() {
        return null;
    }

    @Override
    public StatusResponse getPosStatus(String posId) {
        return null;
    }

    @Override
    public PosConnectionResponse setPosStatus(String id, String status) {
        return null;
    }

    @Override
    public List<PosConnectionResponse> listPosConnection(String userId) {
        return null;
    }

    @Override
    public PosConnectionResponse registerPos(PosConnectionRequest posConnectionRequest) {
        return null;
    }

    @Override
    public PosConnectionResponse connectPos(PosConnectionRequest posConnectionRequest) {
        return null;
    }
}
