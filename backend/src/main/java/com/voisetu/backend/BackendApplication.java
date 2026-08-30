package com.voisetu.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;
import com.voisetu.backend.config.AppProperties;

/**
 * Words2Voice — TTS Platform Backend.
 *
 * {@code @EnableRetry} activates Spring Retry for {@code @Retryable} on {@link SupertonicClient}.
 * {@code @EnableConfigurationProperties} registers the typed {@link AppProperties} bean.
 */
@SpringBootApplication
@EnableRetry
@EnableConfigurationProperties(AppProperties.class)
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
