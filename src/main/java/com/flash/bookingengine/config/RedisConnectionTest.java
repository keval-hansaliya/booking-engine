package com.flash.bookingengine.config;

import ch.qos.logback.core.spi.PropertyContainer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisConnectionTest implements CommandLineRunner {

    private final StringRedisTemplate redisTemplate;
    private final Environment environment;

    @Override
    public void run(String... args) {
        try {
            log.info("Testing connection to Upstash Redis...");

            // Read values resolved by Spring Boot
            String host = environment.getProperty("spring.data.redis.host");
            String port = environment.getProperty("spring.data.redis.port");
            String username = environment.getProperty("spring.data.redis.username");
            String password = environment.getProperty("spring.data.redis.password");

            System.out.println("========== REDIS CONFIGURATION ==========");
            System.out.println("Redis Host: " + host);
            System.out.println("Redis Port: " + port);
            System.out.println("Redis Username: " + username);
            System.out.println("Redis Password Loaded: "
                    + (password != null && !password.isBlank()));
            System.out.println("=========================================");

            // 1. Write a test key
            redisTemplate.opsForValue().set("flash:ping", "pong");

            // 2. Read it back
            String result = redisTemplate.opsForValue().get("flash:ping");

            log.info("==========================================");
            log.info("REDIS CONNECTION SUCCESSFUL! Value: {}", result);
            log.info("==========================================");
        } catch (Exception e) {
            log.error("FAILED TO CONNECT TO UPSTASH REDIS: {}", e.getMessage(), e);
        }
    }
}