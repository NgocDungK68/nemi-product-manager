package com.nemi.model.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("sapo")
public class SapoConfig {
    private String clientId;
    private String clientSecret;
    private String accessToken;
    private String url;
}
