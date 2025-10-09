package com.nemi.configuration;

import com.nemi.enums.Status;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Data
@Configuration
@ConfigurationProperties("nhanhvn")
public class NhanhvnConfig {
    private String baseUrl;
    private String urlAccessCode;
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
        private HashMap<Integer, String> mapping;
    }
    public String getStatusMapping(Integer key) {

        return Optional.ofNullable(key)
                .map(code -> this.getOrder().getStatus().getMapping()
                        .get(code))
                .orElse(Status.UNKNOWN.getValue());
    }
}
