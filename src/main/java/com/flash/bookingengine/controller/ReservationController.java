package com.flash.bookingengine.controller;

import com.flash.bookingengine.dto.ReservationRequestDto;
import com.flash.bookingengine.model.Reservation;
import com.flash.bookingengine.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<?> createReservation(@Valid @RequestBody ReservationRequestDto request) {
        try {
            Reservation reservation = reservationService.reserveTicket(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}