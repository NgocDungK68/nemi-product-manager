package com.nemi.service;

import com.nemi.repository.PosRepository;
import com.nemi.util.ClaimUtil;
import org.springframework.stereotype.Service;

@Service
public class GeneralPosService extends AbstractPosManagementService{

    public GeneralPosService(PosRepository posRepository, ClaimUtil claimUtil) {
        super(posRepository, claimUtil);
    }

    // Inherit all common methods from AbstractPosManagementService:
    // - getPosStatus(String posId)
    // - setPosStatus(ChangeStatusRequest changeStatusRequest)
    // - getAllPos()
}
