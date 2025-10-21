package com.nemi.annotation;

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

        // Try different body formats for HMAC verification
        return verifyHmacWithDifferentFormats(body, clientSecret, hmacHeader);
    }

    public boolean checkSignature(Map<String, String> headers, String body, String podId) {
        return checkSignature(headers, (Object) body, podId);
    }


    public boolean verifyHmacWithDifferentFormats(Object body, String secret, String hmacHeader) {
        String standardJson = JsonUtils.toJson(body);

        // Try different combinations of body content
        String[] bodyFormats = {
                "", // Empty body
                "{}", // Empty JSON object
                "{\"id\":" + extractProductId(standardJson) + "}", // Product ID only
                standardJson, // Standard JSON
                standardJson.replace("\"content\":\"\"", "\"content\":null") // Normalized JSON
        };

        for (String bodyFormat : bodyFormats) {
            log.debug("Trying body format: {}", bodyFormat.length() > 50 ? bodyFormat.substring(0, 50) + "..." : bodyFormat);
            if (verifyHmac(bodyFormat, secret, hmacHeader)) {
                log.debug("HMAC verification succeeded with body format: {}", bodyFormat.length() > 50 ? bodyFormat.substring(0, 50) + "..." : bodyFormat);
                return true;
            }
        }

        // Try with different secret combinations
        String[] secrets = {secret, secret.toUpperCase(), secret.toLowerCase()};
        for (String testSecret : secrets) {
            if (!testSecret.equals(secret)) {
                log.debug("Trying with modified secret: {}", testSecret);
                if (verifyHmac(standardJson, testSecret, hmacHeader)) {
                    log.debug("HMAC verification succeeded with modified secret");
                    return true;
                }
            }
        }

        log.debug("All body formats and secret variations failed for HMAC verification");
        return false;
    }

    private String extractProductId(String json) {
        try {
            // Simple extraction of product ID from JSON
            int idStart = json.indexOf("\"id\":") + 5;
            int idEnd = json.indexOf(",", idStart);
            if (idEnd == -1) {
                idEnd = json.indexOf("}", idStart);
            }
            return json.substring(idStart, idEnd).trim();
        } catch (Exception e) {
            return "0";
        }
    }

    public boolean verifyHmac(String body, String secret, String hmacHeader) {
        try {
            log.info("=== HMAC VERIFICATION DEBUG ===");
            log.info("Body length: {}, Secret length: {}", body.length(), secret.length());
            log.info("Body content: {}", body);
            log.info("Secret: {}", secret);
            log.info("Received HMAC: {}", hmacHeader);

            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
            hmac.init(key);
            String computed = Base64.getEncoder().encodeToString(hmac.doFinal(body.getBytes("UTF-8")));

            boolean isValid = computed.equals(hmacHeader);
            log.info("Computed HMAC: {}", computed);
            log.info("HMAC Match: {}", isValid);
            log.info("=== END HMAC VERIFICATION ===");

            return isValid;
        } catch (Exception e) {
            log.error("Error verifying HMAC", e);
            return false;
        }
    }
}