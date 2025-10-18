package com.nemi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "webhook")
public class WebhookWhitelistConfig {

    // Map<String, List<String>> để map các nhóm (nhanhvn, pancake, sapo)
    private Map<String, List<String>> allowedIps;

    public List<String> getIpsForPartner(String partner) {
        return allowedIps.get(partner);
    }

    public boolean isAllowedIp(String partner, String ip) {
        List<String> ips = allowedIps.get(partner);
        return ips != null && ips.contains(ip);
    }
}