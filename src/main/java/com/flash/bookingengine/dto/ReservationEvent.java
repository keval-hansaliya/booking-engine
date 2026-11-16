package com.flash.bookingengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationEvent implements Serializable {
    private String reservationToken;
    private Long itemId;
    private String userId;
    private LocalDateTime timestamp;
}