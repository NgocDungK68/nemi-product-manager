package com.nemi.configuration;

import com.nemi.enums.Status;
import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Optional;

@Data
@Configuration
@ConfigurationProperties("sapo")
public class SapoConfig {
    private String clientId;
    private String clientSecret;
    private String url;
    private BatchConfig sync;
    private Integer recentDays;
    private Boolean isSyncAllProduct;    // added
    private Boolean isSyncAllOrder;      // added
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

    @Getter
    private static SapoConfig instance;

    @Data
    public static class BatchConfig {
        private int pageStart;
        private int productLimit;
        private int batchSize;
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

    public String getStatusMapping(String key) {

        return Optional.ofNullable(key)
                .map(code -> this.getOrder().getStatus().getMapping()
                        .get(code))
                .orElse(Status.UNKNOWN.getValue());
    }
}
