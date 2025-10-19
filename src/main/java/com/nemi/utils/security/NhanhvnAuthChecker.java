package com.nemi.utils.security;

import com.nemi.constant.NhanhvnConstants;
import com.nemi.constant.PancakeConstatns;
import com.nemi.entity.PosEntity;
import com.nemi.repository.PosRepository;
import com.nemi.service.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
@RequiredArgsConstructor
@Slf4j
@Component("nhanhvnAuth")
public class NhanhvnAuthChecker {
    private final PosRepository posRepository;
    private final EncryptionService encryptionService;
    public boolean checkWebhookToken(Map<String, String> headers, String posId) {



        String webhookToken = headers.get(NhanhvnConstants.AUTHORIZATION);
        if (webhookToken == null){
            log.warn("Missing webhook token (posId={})", posId);
            return false;
        }
        log.info("Checking webhook token for posId={}", posId);
        return posRepository.findById(posId)
                .map(PosEntity::getWebhookToken)
                .map(encryptionService::decrypt)
                .map(webhookToken::equals)
                .orElse(false);

    }
}
