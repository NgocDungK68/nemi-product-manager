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

    public boolean checkSignature(Map<String, String> headers, Object body, String posId) {
        log.debug("SapoAuthChecker.checkSignature called with posId={}", posId);

        String hmacHeader = headers.get(SapoConstants.X_SAPO_SIGNATURE);
        if (hmacHeader == null) {
            log.warn("Missing signature header '{}' (posId={})", SapoConstants.X_SAPO_SIGNATURE, posId);
            return false;
        }

        String clientSecret = sapoConfig.getClientSecret();
        if (ObjectUtils.isEmpty(clientSecret)) {
            log.warn("Client secret not found in SapoConfig for posId={}", posId);
            return false;
        }

        log.debug("Using client secret for HMAC verification");

        String normalizedBody = normalizeJson(JsonUtils.toJson(body));
        return verifyHmac(normalizedBody, clientSecret, hmacHeader);
    }

    public boolean checkSignature(Map<String, String> headers, String body, String posId) {
        return checkSignature(headers, (Object) body, posId);
    }

    /**
     * Chuẩn hóa JSON để loại bỏ khác biệt về format giữa "" và null, 0.0 và 0
     */
    private String normalizeJson(String json) {
        if (json == null) return "";

        String normalized = json;
        
        // 1. Chuyển content rỗng "" -> null
        normalized = normalized.replace("\"content\":\"\"", "\"content\":null");
        
        // 2. Chuyển TẤT CẢ số dạng X.0 -> X (dùng regex)
        // Ví dụ: "price":0.0 → "price":0, "weight":0.0 → "weight":0
        normalized = normalized.replaceAll(":(\\d+)\\.0(?=[,}])", ":$1");
        
        // 3. Loại bỏ spaces sau colon (nếu Jackson thêm vào)
        normalized = normalized.replaceAll(": ", ":");
        
        // 4. Loại bỏ khoảng trắng thừa đầu/cuối
        normalized = normalized.trim();

        log.debug("Normalized JSON for HMAC: {}", normalized.length() > 200 ? normalized.substring(0, 200) + "..." : normalized);
        return normalized;
    }

    private boolean verifyHmac(String body, String secret, String hmacHeader) {
        try {
            log.info("=== HMAC VERIFICATION DEBUG ===");
            log.info("Body: {}", body);
            log.info("Secret length: {}", secret.length());
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
