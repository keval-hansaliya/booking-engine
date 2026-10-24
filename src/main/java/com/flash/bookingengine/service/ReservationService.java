package com.flash.bookingengine.service;

import com.flash.bookingengine.dto.ReservationRequestDto;
import com.flash.bookingengine.model.Item;
import com.flash.bookingengine.model.Reservation;
import com.flash.bookingengine.model.ReservationStatus;
import com.flash.bookingengine.repository.ItemRepository;
import com.flash.bookingengine.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ItemRepository itemRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public Reservation reserveTicket(ReservationRequestDto request) {
        // 1. Fetch item from Neon DB
        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("Item not found with ID: " + request.getItemId()));

        // 2. Naive inventory check
        if (item.getAvailableStock() <= 0) {
            throw new IllegalStateException("Sold out! No tickets remaining.");
        }

        // 3. Decrement stock
        item.setAvailableStock(item.getAvailableStock() - 1);
        itemRepository.save(item);

        // 4. Create and save reservation
        Reservation reservation = Reservation.builder()
                .itemId(item.getId())
                .userId(request.getUserId())
                .status(ReservationStatus.PENDING)
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);
        log.info("Reserved ticket ID={} for user={}, remaining stock={}",
                item.getId(), request.getUserId(), item.getAvailableStock());

        return savedReservation;
    }
}