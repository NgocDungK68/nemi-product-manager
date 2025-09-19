package com.nemi.model.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("nhanhvn")
public class NhanhvnConfig {
    private String url;
    private Integer appId;
    private Integer businessId;
    private String accessToken;
    private String apiVersion;
    private String secretKey;
}
