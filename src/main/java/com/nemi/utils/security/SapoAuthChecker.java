package com.nemi.utils.security;

import com.nemi.configuration.SapoConfig;
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
    private final SapoConfig sapoConfig;

    public boolean checkSignature(Map<String, String> headers, Object body, String podId) {
        log.debug("SapoAuthChecker.checkSignature called with posId={}", podId);
        log.debug("Headers received: {}", headers);
        log.debug("Body received: {}", body);
        
        // Convert Object body to proper JSON string for HMAC verification
        String bodyJson = JsonUtils.toJson(body);
        log.debug("Body as JSON: {}", bodyJson);
        
        String hmacHeader = headers.get(SapoConstants.X_SAPO_SIGNATURE);
        if (hmacHeader == null) {
            log.warn("Missing signature header '{}' (posId={}). Available headers: {}", 
                    SapoConstants.X_SAPO_SIGNATURE, podId, headers.keySet());
            return false;
        }
        
        log.debug("Found signature header '{}' with value: {}", SapoConstants.X_SAPO_SIGNATURE, hmacHeader);

        // Check if POS entity exists
        if (!posRepository.findById(podId).isPresent()) {
            log.warn("POS entity not found for posId={}", podId);
            return false;
        }
        
        // For Sapo webhooks, we might need to use client secret instead of access token
        // Let's try both approaches
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
        
        if (!isValid) {
            log.debug("HMAC verification failed with access token, trying with other secrets...");
            
            // Try with client secret
            String clientSecret = sapoConfig.getClientSecret();
            isValid = verifyHmac(bodyJson, clientSecret, hmacHeader);
            if (isValid) {
                log.debug("HMAC verification succeeded with client secret");
            }
            
            // Try with other possible secrets
            if (!isValid) {
                String masterSecret = sapoConfig.getAccessToken(); // Try master token
                isValid = verifyHmac(bodyJson, masterSecret, hmacHeader);
                if (isValid) {
                    log.debug("HMAC verification succeeded with master token");
                }
            }
            
            // Try with a combination of secrets
            if (!isValid) {
                String combinedSecret = clientSecret + accessToken;
                isValid = verifyHmac(bodyJson, combinedSecret, hmacHeader);
                if (isValid) {
                    log.debug("HMAC verification succeeded with combined secret");
                }
            }
        }
        log.debug("HMAC verification result: {}", isValid);
        return isValid;
    }

    public boolean checkSignature(Map<String, String> headers, String body, String podId) {
        return checkSignature(headers, (Object) body, podId);
    }


    public boolean verifyHmac(String body, String accessToken, String hmacHeader) {
        try {
            // Try different approaches for HMAC verification
            log.debug("=== HMAC Verification Attempts ===");
            
            // Method 1: Standard HMAC-SHA256 with UTF-8 encoding
            boolean method1 = verifyHmacMethod(body, accessToken, hmacHeader, "UTF-8", "Method 1 (UTF-8)");
            if (method1) return true;
            
            // Method 2: Try with different character encoding
            boolean method2 = verifyHmacMethod(body, accessToken, hmacHeader, "ISO-8859-1", "Method 2 (ISO-8859-1)");
            if (method2) return true;
            
            // Method 3: Try with raw bytes (no encoding)
            boolean method3 = verifyHmacRawBytes(body, accessToken, hmacHeader);
            if (method3) return true;
            
            // Method 4: Try with hex encoding instead of base64
            boolean method4 = verifyHmacHex(body, accessToken, hmacHeader);
            if (method4) return true;
            
            log.debug("All HMAC verification methods failed");
            return false;
            
        } catch (Exception e) {
            log.error("Error verifying HMAC", e);
            return false;
        }
    }
    
    private boolean verifyHmacMethod(String body, String secret, String receivedHmac, String encoding, String methodName) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(secret.getBytes(encoding), "HmacSHA256");
            hmac.init(key);
            String computed = Base64.getEncoder().encodeToString(hmac.doFinal(body.getBytes(encoding)));
            
            log.debug("  {}: Computed={}, Received={}, Match={}", 
                    methodName, computed, receivedHmac, computed.equals(receivedHmac));
            
            return computed.equals(receivedHmac);
        } catch (Exception e) {
            log.debug("  {}: Failed with exception: {}", methodName, e.getMessage());
            return false;
        }
    }
    
    private boolean verifyHmacRawBytes(String body, String secret, String receivedHmac) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            hmac.init(key);
            String computed = Base64.getEncoder().encodeToString(hmac.doFinal(body.getBytes()));
            
            log.debug("  Method 3 (Raw bytes): Computed={}, Received={}, Match={}", 
                    computed, receivedHmac, computed.equals(receivedHmac));
            
            return computed.equals(receivedHmac);
        } catch (Exception e) {
            log.debug("  Method 3: Failed with exception: {}", e.getMessage());
            return false;
        }
    }
    
    private boolean verifyHmacHex(String body, String secret, String receivedHmac) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            hmac.init(key);
            
            // Convert hex string to bytes for comparison
            byte[] receivedBytes = Base64.getDecoder().decode(receivedHmac);
            String computedHex = bytesToHex(hmac.doFinal(body.getBytes()));
            String receivedHex = bytesToHex(receivedBytes);
            
            log.debug("  Method 4 (Hex): Computed={}, Received={}, Match={}", 
                    computedHex, receivedHex, computedHex.equals(receivedHex));
            
            return computedHex.equals(receivedHex);
        } catch (Exception e) {
            log.debug("  Method 4: Failed with exception: {}", e.getMessage());
            return false;
        }
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}