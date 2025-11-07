package com.realtime.communication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main Spring Boot Application for Real-Time Communication Platform
 */
@SpringBootApplication
@EnableCaching
@EnableKafka
@EnableAsync
public class CommunicationPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommunicationPlatformApplication.class, args);
    }
}
