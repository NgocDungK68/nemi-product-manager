package com.nemi.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("async")
public class AsyncProperties {
    private ExecutorConfig sync;
    private ExecutorConfig batch;

    @Data
    public static class ExecutorConfig {
        private int corePoolSize;
        private int maxPoolSize;
        private int queueCapacity;
    }
}
