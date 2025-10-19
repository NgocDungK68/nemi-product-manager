package com.nemi.utils.security;

import com.nemi.configuration.SapoConfig;
import com.nemi.constant.SapoConstants;
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
    private final SapoConfig sapoConfig;

    public boolean checkSignature(Map<String, String> headers, Object body, String podId) {
        log.debug("SapoAuthChecker.checkSignature called with posId={}", podId);
        
        // Convert Object body to proper JSON string for HMAC verification
        String bodyJson = JsonUtils.toJson(body);
        
        String hmacHeader = headers.get(SapoConstants.X_SAPO_SIGNATURE);
        if (hmacHeader == null) {
            log.warn("Missing signature header '{}' (posId={})", SapoConstants.X_SAPO_SIGNATURE, podId);
            return false;
        }
        
        // Sapo uses client_secret from config for HMAC verification
        String clientSecret = sapoConfig.getClientSecret();
        if (ObjectUtils.isEmpty(clientSecret)) {
            log.warn("Client secret not found in SapoConfig for posId={}", podId);
            return false;
        }
        
        log.debug("Using client secret for HMAC verification: {}", clientSecret);
        return verifyHmac(bodyJson, clientSecret, hmacHeader);
    }

    public boolean checkSignature(Map<String, String> headers, String body, String podId) {
        return checkSignature(headers, (Object) body, podId);
    }


    public boolean verifyHmac(String body, String secret, String hmacHeader) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
            hmac.init(key);
            String computed = Base64.getEncoder().encodeToString(hmac.doFinal(body.getBytes("UTF-8")));
            
            boolean isValid = computed.equals(hmacHeader);
            log.debug("HMAC verification: Computed={}, Received={}, Match={}", 
                    computed, hmacHeader, isValid);
            
            return isValid;
        } catch (Exception e) {
            log.error("Error verifying HMAC", e);
            return false;
        }
    }
}