package com.flash.bookingengine.service;

import com.flash.bookingengine.dto.ReservationRequestDto;
import com.flash.bookingengine.model.Reservation;
import com.flash.bookingengine.model.ReservationStatus;
import com.flash.bookingengine.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisReservationService {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> decrementStockScript;
    private final ReservationRepository reservationRepository;

    public Reservation reserveWithRedis(ReservationRequestDto request) {
        String stockKey = "item:" + request.getItemId() + ":stock";

        // 1. Atomic Lua gatekeeper check in RAM
        Long remainingStock = redisTemplate.execute(
                decrementStockScript,
                Collections.singletonList(stockKey),
                "1"
        );

        if (remainingStock == null || remainingStock == -2) {
            throw new IllegalArgumentException("Item inventory not loaded in Redis for ID: " + request.getItemId());
        }

        if (remainingStock == -1) {
            // Rejected immediately in memory; no database transaction is created
            throw new IllegalStateException("Sold out! No tickets remaining.");
        }

        // 2. Persist reservation only for successful allocations
        Reservation reservation = Reservation.builder()
                .itemId(request.getItemId())
                .userId(request.getUserId())
                .status(ReservationStatus.PENDING)
                .build();

        return reservationRepository.save(reservation);
    }
}