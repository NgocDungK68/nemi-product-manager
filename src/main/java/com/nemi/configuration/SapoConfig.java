package com.nemi.configuration;

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
    private String baseUrl;
    private String storeName;
    private Webhook webhook;
    private String urlRegisterWebhook;
    
    // API Paths
    private String pathOauthAccessToken;
    private String pathProducts;
    private String pathWebhooks;

    @Data
    public static class Webhook {
        private java.util.List<String> topic;
    }
}
