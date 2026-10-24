package com.flash.bookingengine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReservationRequestDto {

    @NotNull(message = "Item ID is required")
    private Long itemId;

    @NotBlank(message = "User ID is required")
    private String userId;
}