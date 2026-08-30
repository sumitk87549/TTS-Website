package com.voisetu.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async task executor configuration for TTS synthesis work.
 *
 * TTS calls block for up to 120 seconds. Without a dedicated pool they would
 * starve the server's HTTP thread pool under concurrent load.
 *
 * Pool sizing:
 *   corePoolSize  = 3  → 3 TTS threads always alive
 *   maxPoolSize   = 5  → spike capacity
 *   queueCapacity = 10 → extra requests wait briefly; beyond this → rejection
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "ttsTaskExecutor")
    public Executor ttsTaskExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(3);
        exec.setMaxPoolSize(5);
        exec.setQueueCapacity(10);
        exec.setThreadNamePrefix("tts-worker-");
        exec.setKeepAliveSeconds(30);
        exec.setAwaitTerminationSeconds(60);
        exec.setWaitForTasksToCompleteOnShutdown(true);
        exec.initialize();
        return exec;
    }
}
