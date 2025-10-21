package com.nemi.configuration;

import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "webhook")
public class WebhookConfig {


    private Map<String, List<String>> allowedIps;

    public List<String> getIpsForPartner(String partner) {
        return allowedIps.get(partner);
    }

    public boolean isAllowedIp(String partner, String ip) {
        List<String> ips = allowedIps.get(partner);
        return ips != null && ips.contains(ip);
    }

    private RateLimit rateLimit;

    // -------------------------------
    // Nested classes cho rate limit
    // -------------------------------

    @Data
    public static class RateLimit {
        private boolean enable;
        private Map<String, PartnerLimit> partners;
    }

    @Data
    public static class PartnerLimit {
        private int limit;         // Số request tối đa trong 1 cửa sổ
        private long windowSize;   // Thời gian cửa sổ (ms)
        private long segmentSize;  // Độ dài mỗi đoạn nhỏ (ms)
    }

    public PartnerLimit getPartnerLimitOrThrow(String partner) {

        PartnerLimit cfg = rateLimit.getPartners().get(partner);
        if (ObjectUtils.isEmpty(cfg)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No rate limit configuration found for partner: " + partner);
        }

        return cfg;
    }


}