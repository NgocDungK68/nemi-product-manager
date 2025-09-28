package com.nemi.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("nhanhvn")
public class NhanhvnConfig {
    private String baseUrl;
    private String urlAccessCode;
    private String urlAccessToken;
    private String urlProducts;
    private String apiVersion;
    private String verifyToken;
    private String secretKey;
}
