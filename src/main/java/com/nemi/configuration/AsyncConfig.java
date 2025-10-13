package com.nemi.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "syncExecutor")
    public ThreadPoolTaskExecutor syncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);      // 10 luồng chạy thường trự
        executor.setMaxPoolSize(20);       // tối đa mở rộng 20
        executor.setQueueCapacity(200);    // hàng đợi chờ tối đa 200 task
        executor.setThreadNamePrefix("SyncExecutor-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "batchExecutor")
    public ThreadPoolTaskExecutor batchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("BatchExecutor-");
        executor.initialize();
        return executor;
    }
}