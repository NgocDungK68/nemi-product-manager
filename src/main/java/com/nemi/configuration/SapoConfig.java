package com.nemi.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties("sapo")
public class SapoConfig {
    private String clientId;
    private String clientSecret;
    private String accessToken;
    private String url;
    private BatchConfig sync;
    //---status-----

    private OrderConfig order;

    @Data
    public static class OrderConfig {
        private StatusConfig status;

        @Data
        public static class StatusConfig {
            private Map<String, String> mapping;
        }
    }
    @Data
    public static class BatchConfig {
        private int order;
        private int orderItem;
        private int product;
        private int pageStart;
    }
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
