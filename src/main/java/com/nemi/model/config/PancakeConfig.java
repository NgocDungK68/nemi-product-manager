package com.nemi.model.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("pancake")
public class PancakeConfig {
    private String baseUrl;
    private String apiKey;
    private String shopId;
    private int timeout;

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

}
