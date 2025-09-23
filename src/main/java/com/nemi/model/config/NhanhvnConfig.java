package com.nemi.model.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("nhanhvn")
public class NhanhvnConfig {
    @Value("${nhanhvn.url.get-access-code}")
    private String urlAccessCode;

    @Value("${nhanhvn.url.get-access-token}")
    private String urlAccessToken;

    private Integer appId;
    private Integer businessId;
    private String accessToken;
    @Value("${api-version}")
    private String apiVersion;
    private String secretKey;
}
