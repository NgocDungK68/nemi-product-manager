package com.nemi.service.impl.nhanhvn;

import com.nemi.constant.enums.Platform;
import com.nemi.model.PosConnection;
import com.nemi.service.PosManagementService;

import java.util.List;

public class NhanhvnServiceImpl implements PosManagementService {
    @Override
    public String getPosManagementType() {
        return Platform.NHANHVN.name();
    }

    @Override
    public String setPosStatus(String status) {
        return null;
    }

    @Override
    public List<PosConnection> listPosConnection(String posName, Long userId) {
        return null;
    }


}
