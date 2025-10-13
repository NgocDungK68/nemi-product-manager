package com.nemi.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Optional;

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
    private SyncConfig sync;

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
        private HashMap<Integer, String> mapping;
    }
    public String getOrderStatusMapping(Integer key) {

        return Optional.ofNullable(key)
                .map(code -> this.getOrder().getStatus().getMapping()
                        .get(code))
                .orElse(null);
    }

    public String getProductStatusMapping(Integer key) {

        return Optional.ofNullable(key)
                .map(code -> this.getProduct().getStatus().getMapping()
                        .get(code))
                .orElse(null);
    }

    @Data
    public static class SyncConfig {
        private int batchSize;
        private int pageSize;
    }
}
