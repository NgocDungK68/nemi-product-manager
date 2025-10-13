package com.nemi.configuration;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AsyncConfig {
    private final AsyncProperties asyncProperties;

    @Bean(name = "syncExecutor")
    public ThreadPoolTaskExecutor syncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncProperties.getSync().getCorePoolSize());      // 10 luồng chạy thường trự
        executor.setMaxPoolSize(asyncProperties.getSync().getMaxPoolSize());        // tối đa mở rộng 20
        executor.setQueueCapacity(asyncProperties.getSync().getQueueCapacity());    // hàng đợi chờ tối đa 200 task
        executor.setThreadNamePrefix("SyncExecutor-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "batchExecutor")
    public ThreadPoolTaskExecutor batchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncProperties.getBatch().getCorePoolSize());
        executor.setMaxPoolSize(asyncProperties.getBatch().getMaxPoolSize());
        executor.setQueueCapacity(asyncProperties.getBatch().getQueueCapacity());
        executor.setThreadNamePrefix("BatchExecutor-");
        executor.initialize();
        return executor;
    }
}