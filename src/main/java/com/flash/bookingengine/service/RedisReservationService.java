package com.flash.bookingengine.service;

import com.flash.bookingengine.dto.ReservationEvent;
import com.flash.bookingengine.dto.ReservationRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisReservationService {

    private static final String TOPIC_RESERVATIONS = "reservation-events";

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> decrementStockScript;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public String reserveWithRedis(ReservationRequestDto request) {
        String stockKey = "item:" + request.getItemId() + ":stock";

        // 1. In-memory atomic check & decrement
        Long remainingStock = redisTemplate.execute(
                decrementStockScript,
                Collections.singletonList(stockKey),
                "1"
        );

        if (remainingStock == null || remainingStock == -2) {
            throw new IllegalArgumentException("Item inventory not loaded in Redis for ID: " + request.getItemId());
        }

        if (remainingStock == -1) {
            throw new IllegalStateException("Sold out! No tickets remaining.");
        }

        // 2. Generate unique reservation token
        String reservationToken = UUID.randomUUID().toString();

        // 3. Build Kafka event
        ReservationEvent event = ReservationEvent.builder()
                .reservationToken(reservationToken)
                .itemId(request.getItemId())
                .userId(request.getUserId())
                .timestamp(LocalDateTime.now())
                .build();

        // 4. Publish to Kafka (partitioned by itemId to ensure strict per-item ordering)
        kafkaTemplate.send(TOPIC_RESERVATIONS, String.valueOf(request.getItemId()), event);
        log.info("Published reservation event to Kafka: Token={}, ItemId={}", reservationToken, request.getItemId());

        return reservationToken;
    }
}