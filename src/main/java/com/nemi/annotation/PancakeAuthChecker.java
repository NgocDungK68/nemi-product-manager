package com.nemi.annotation;

import com.nemi.constant.PancakeConstatns;
import com.nemi.entity.PosEntity;
import com.nemi.repository.PosRepository;
import com.nemi.service.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("pancakeAuth")
@RequiredArgsConstructor
@Slf4j
public class PancakeAuthChecker {

    private final PosRepository posRepository;
    private final EncryptionService encryptionService;

    public boolean checkWebhookToken(Map<String, String> headers, String posId) {

        String webhookToken = headers.get(PancakeConstatns.WEBHOOK_TOKEN);
        if (ObjectUtils.isEmpty(webhookToken)) {
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
