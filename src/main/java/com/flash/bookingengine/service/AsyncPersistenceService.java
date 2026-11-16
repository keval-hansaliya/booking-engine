/*
package com.flash.bookingengine.service;

import com.flash.bookingengine.model.Reservation;
import com.flash.bookingengine.model.ReservationStatus;
import com.flash.bookingengine.repository.ItemRepository;
import com.flash.bookingengine.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncPersistenceService {

    private final ReservationRepository reservationRepository;
    private final ItemRepository itemRepository;

    @Async
    public void persistReservationAndSyncStock(Long itemId, String userId) {
        try {
            // 1. Persist the reservation row
            Reservation reservation = Reservation.builder()
                    .itemId(itemId)
                    .userId(userId)
                    .status(ReservationStatus.PENDING)
                    .build();
            reservationRepository.save(reservation);

            // 2. Decrement the DB available_stock atomically
            itemRepository.decrementAvailableStock(itemId);

            log.info("Async DB sync completed for User={}, ItemId={}", userId, itemId);
        } catch (Exception e) {
            log.error("Failed to persist reservation asynchronously for User={}, ItemId={}: {}",
                    userId, itemId, e.getMessage(), e);
        }
    }
}
 */