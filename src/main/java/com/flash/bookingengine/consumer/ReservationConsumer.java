package com.flash.bookingengine.consumer;

import com.flash.bookingengine.dto.ReservationEvent;
import com.flash.bookingengine.model.Reservation;
import com.flash.bookingengine.model.ReservationStatus;
import com.flash.bookingengine.repository.ItemRepository;
import com.flash.bookingengine.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationConsumer {

    private final ReservationRepository reservationRepository;
    private final ItemRepository itemRepository;

    @KafkaListener(topics = "reservation-events", groupId = "booking-persistence-group")
    @Transactional
    public void consumeReservationEvent(ReservationEvent event) {
        log.info("Consuming reservation event from Kafka: Token={}, User={}, ItemId={}",
                event.getReservationToken(), event.getUserId(), event.getItemId());

        try {
            // 1. Persist the reservation row
            Reservation reservation = Reservation.builder()
                    .itemId(event.getItemId())
                    .userId(event.getUserId())
                    .status(ReservationStatus.PENDING)
                    .build();
            reservationRepository.save(reservation);

            // 2. Update stock count in PostgreSQL
            itemRepository.decrementAvailableStock(event.getItemId());

            log.info("Successfully persisted reservation and decremented DB stock for Token={}", 
                    event.getReservationToken());
        } catch (Exception e) {
            log.error("Failed to process reservation event for Token={}: {}", 
                    event.getReservationToken(), e.getMessage(), e);
            throw e; // Rethrow to let Kafka retry or route to DLQ
        }
    }
}