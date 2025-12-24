package com.tcpviewer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Application configuration for Spring beans.
 * Configures thread pools for TCP proxy operations.
 */
@Configuration
public class AppConfig {

    @Value("${app.proxy.thread-pool.core-size:10}")
    private int corePoolSize;

    @Value("${app.proxy.thread-pool.max-size:100}")
    private int maxPoolSize;

    @Value("${app.proxy.thread-pool.queue-capacity:50}")
    private int queueCapacity;

    /**
     * Thread pool executor for handling TCP proxy connections.
     * Each connection handler runs in this pool.
     */
    @Bean(name = "proxyExecutor")
    public Executor proxyExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("proxy-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
