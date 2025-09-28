package com.nemi.service;

import com.nemi.entity.SyncHistoryEntity;
import com.nemi.repository.PosRepository;
import com.nemi.repository.SyncHistoryRepository;
import com.nemi.util.ClaimUtil;
import org.springframework.stereotype.Service;

@Service
public class GeneralPosService extends AbstractPosManagementService{

    public GeneralPosService(PosRepository posRepository, ClaimUtil claimUtil, SyncHistoryRepository syncHistoryRepository) {
        super(posRepository, claimUtil,syncHistoryRepository);
    }

    // Inherit all common methods from AbstractPosManagementService:
    // - getPosStatus(String posId)
    // - setPosStatus(ChangeStatusRequest changeStatusRequest)
    // - getAllPos()
}
