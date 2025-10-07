package com.nemi.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties("pancake")
public class PancakeConfig {
    private String baseUrl;
    private String apiKey;
    private String shopId;
    private int timeout;
    private BatchConfig sync;
    private String xApiKey;

    @Data
    public static class Retry {
        private int maxAttempts;
        private int backoffDelay;
    }

    @Data
    public static class Sync {
        private int pageSize;
        private int batchSize;
        private int maxConcurrent;
    }

    //---status-----
    private OrderConfig order;

    @Data
    public static class OrderConfig {
        private StatusConfig status;

        @Data
        public static class StatusConfig {
            private Map<Integer, String> mapping;
        }
    }

    @Data
    public static class BatchConfig {
        private int order;
        private int orderItem;
        private int product;
        private int pageStart;
    }

}
