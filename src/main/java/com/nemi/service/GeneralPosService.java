package com.nemi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nemi.configuration.NhanhvnConfig;
import com.nemi.configuration.WebhookConfig;
import com.nemi.repository.PosRepository;
import com.nemi.repository.SyncHistoryRepository;
import com.nemi.service.factory.ReAuthPosFactory;
import com.nemi.util.ClaimUtil;
import org.springframework.stereotype.Service;

@Service
public class GeneralPosService extends AbstractPosManagementService {

    public GeneralPosService(PosRepository posRepository, ClaimUtil claimUtil,
                             SyncHistoryRepository syncHistoryRepository, ObjectMapper objectMapper, NhanhvnConfig nhanhvnConfig, WebhookConfig webhookConfig, ReAuthPosFactory reAuthPosFactory) {
        super(posRepository, claimUtil, syncHistoryRepository, objectMapper, nhanhvnConfig, webhookConfig,reAuthPosFactory);
    }

    // Inherit all common methods from AbstractPosManagementService:
    // - getPosStatus(String posId)
    // - setPosStatus(ChangeStatusRequest changeStatusRequest)
    // - getAllPos()
}
