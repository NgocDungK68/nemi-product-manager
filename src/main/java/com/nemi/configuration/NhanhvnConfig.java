package com.nemi.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties("nhanhvn")
public class NhanhvnConfig {
    private String baseUrl;
    private String posUrl;
    private String returnLink;
    private String urlOauth;
    private String urlAccessToken;
    private String urlProducts;
    private String urlOrders;
    private String apiVersion;
    private String verifyToken;
    private String secretKey;
    private ProductConfig product;
    private OrderConfig order;

    @Data
    public static class ProductConfig {
        private StatusConfig status;
    }

    @Data
    public static class OrderConfig {
        private StatusConfig status;
    }

    @Data
    public static class StatusConfig {
        private Map<Integer, String> mapping;
    }
}
