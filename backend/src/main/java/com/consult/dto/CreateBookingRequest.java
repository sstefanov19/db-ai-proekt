package com.consult.dto;

import jakarta.validation.constraints.NotNull;

public record CreateBookingRequest(
    @NotNull(message = "slotId е задължителен.")
    Long slotId
) {}
