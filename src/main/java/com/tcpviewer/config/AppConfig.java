package com.tcpviewer.config;

import com.tcpviewer.io.wrapper.factory.DefaultServerSocketFactory;
import com.tcpviewer.io.wrapper.factory.DefaultSocketFactory;
import com.tcpviewer.io.wrapper.factory.ServerSocketFactory;
import com.tcpviewer.io.wrapper.factory.SocketFactory;
import com.tcpviewer.javafx.wrapper.PlatformWrapper;
import com.tcpviewer.javafx.wrapper.impl.DefaultPlatformWrapper;
import com.tcpviewer.lang.wrapper.factory.DefaultExecutorServiceFactory;
import com.tcpviewer.lang.wrapper.factory.DefaultThreadFactory;
import com.tcpviewer.lang.wrapper.factory.ExecutorServiceFactory;
import com.tcpviewer.lang.wrapper.factory.ThreadFactory;
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

    /**
     * Socket factory for creating and wrapping sockets.
     * Provides mockable abstraction over JDK Socket classes for testability.
     */
    @Bean
    public SocketFactory socketFactory() {
        return new DefaultSocketFactory();
    }

    /**
     * Thread factory for creating and managing thread wrappers.
     * Provides mockable abstraction over JDK Thread for testability.
     */
    @Bean
    public ThreadFactory threadFactory() {
        return new DefaultThreadFactory();
    }

    /**
     * Executor service factory for creating thread pool wrappers.
     * Provides mockable abstraction over JDK ExecutorService for testability.
     */
    @Bean
    public ExecutorServiceFactory executorServiceFactory(ThreadFactory threadFactory) {
        return new DefaultExecutorServiceFactory();
    }

    /**
     * Platform wrapper for JavaFX UI thread synchronization.
     * Provides mockable abstraction over JavaFX Platform for testability.
     */
    @Bean
    public PlatformWrapper platformWrapper() {
        return new DefaultPlatformWrapper();
    }

    /**
     * Server socket factory for creating and wrapping server sockets.
     * Provides mockable abstraction over JDK ServerSocket for testability.
     */
    @Bean
    public ServerSocketFactory serverSocketFactory(SocketFactory socketFactory) {
        return new DefaultServerSocketFactory(socketFactory);
    }
}
