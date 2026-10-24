package com.flash.bookingengine.repository;

import com.flash.bookingengine.model.Reservation;
import com.flash.bookingengine.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    List<Reservation> findByUserId(String userId);
    List<Reservation> findByItemIdAndStatus(Long itemId, ReservationStatus status);
}