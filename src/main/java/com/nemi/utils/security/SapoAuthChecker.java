package com.nemi.utils.security;

import com.nemi.constant.SapoConstants;
import com.nemi.entity.PosEntity;
import com.nemi.repository.PosRepository;
import com.nemi.service.EncryptionService;
import com.nemi.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Map;

@Component("sapoAuth")
@RequiredArgsConstructor
@Slf4j
public class SapoAuthChecker {
    private final PosRepository posRepository;
    private final EncryptionService encryptionService;

    public boolean checkSignature(Map<String, String> headers, Object body, String podId) {
        log.debug("SapoAuthChecker.checkSignature called with posId={}", podId);
        log.debug("Headers received: {}", headers);
        log.debug("Body received: {}", body);
        
        // Convert Object body to proper JSON string for HMAC verification
        String bodyJson = JsonUtils.toJson(body);
        log.debug("Body as JSON: {}", bodyJson);
        
        String hmacHeader = headers.get(SapoConstants.X_SAPO_SIGNATURE);
        if (hmacHeader == null) {
            log.warn("Missing X-SAPO-SIGNATURE (posId={}). Available headers: {}", podId, headers.keySet());
            return false;
        }

        String accessToken = posRepository.findById(podId)
                .map(PosEntity::getAccessToken)
                .map(encryptionService::decrypt)
                .orElse(null);

        if (ObjectUtils.isEmpty(accessToken)) {
            log.warn("Access token not found for posId={}", podId);
            return false;
        }
        
        log.debug("Verifying HMAC with accessToken length={}, hmacHeader={}", 
                accessToken.length(), hmacHeader);
        
        boolean isValid = verifyHmac(bodyJson, accessToken, hmacHeader);
        log.debug("HMAC verification result: {}", isValid);
        return isValid;
    }

    public boolean checkSignature(Map<String, String> headers, String body, String podId) {
        return checkSignature(headers, (Object) body, podId);
    }


    public boolean verifyHmac(String body, String accessToken, String hmacHeader) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(accessToken.getBytes(), "HmacSHA256");
            hmac.init(key);
            String computed = Base64.getEncoder().encodeToString(hmac.doFinal(body.getBytes()));
            
            log.debug("HMAC verification details:");
            log.debug("  Body: {}", body);
            log.debug("  AccessToken: {}", accessToken);
            log.debug("  Computed HMAC: {}", computed);
            log.debug("  Received HMAC: {}", hmacHeader);
            log.debug("  Match: {}", computed.equals(hmacHeader));
            
            return computed.equals(hmacHeader);
        } catch (Exception e) {
            log.error("Error verifying HMAC", e);
            return false;
        }
    }
}