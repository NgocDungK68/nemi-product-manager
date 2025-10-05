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
}
