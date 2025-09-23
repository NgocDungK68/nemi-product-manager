package com.nemi.service_impl.nhanhvn;

import com.nemi.model.request.PosConnectionRequest;
import com.nemi.model.response.PosConnectionResponse;
import com.nemi.service.PosManagementService;

import java.util.List;

public class NhanhvnServiceImpl implements PosManagementService {
    @Override
    public String getPosName() {
        return null;
    }

    @Override
    public String getPosStatus(String transactionId) {
        return null;
    }

    @Override
    public String setPosStatus(String status) {
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
    public PosConnectionResponse connectPos() {
        return null;
    }
}
