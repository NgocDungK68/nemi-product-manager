package com.nemi.annotation;

import com.nemi.filter.SapoWebhookFilter;
import com.nemi.configuration.SapoConfig;
import com.nemi.constant.SapoConstants;
import com.nemi.util.JsonUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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

        // Lấy raw body từ filter cache (đã được SapoWebhookFilter lưu sẵn)
        String rawBody = getRawBodyFromRequest();
        if (rawBody != null) {
            log.info("Using raw body from filter for HMAC verification");
            return verifyHmac(rawBody, clientSecret, hmacHeader);
        }

        // Fallback: dùng parsed body (có thể sai format)
        log.warn("Raw body not found, falling back to parsed body (may fail!)");
        String normalizedBody = normalizeJson(JsonUtils.toJson(body));
        return verifyHmac(normalizedBody, clientSecret, hmacHeader);
    }

    /**
     * Lấy raw body đã được cache bởi SapoWebhookFilter.
     * Filter đã đọc raw body TRỨ KHI Spring parse @RequestBody.
     * 
     * @return raw JSON string từ Sapo, hoặc null nếu không có cache
     */
    private String getRawBodyFromRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                Object rawBody = request.getAttribute(SapoWebhookFilter.CACHED_RAW_BODY_ATTRIBUTE);
                if (rawBody instanceof String) {
                    return (String) rawBody;
                }
            }
        } catch (Exception e) {
            log.debug("Could not retrieve raw body from request attributes: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Chuẩn hóa JSON để loại bỏ khác biệt về format giữa "" và null, 0.0 và 0
     */
    private String normalizeJson(String json) {
        if (json == null) return "";

        String normalized = json;
        // Loại bỏ khoảng trắng thừa (phòng trường hợp JSON không chuẩn)
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
