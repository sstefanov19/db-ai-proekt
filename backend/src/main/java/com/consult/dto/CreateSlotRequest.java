package com.consult.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateSlotRequest(
    @NotNull(message = "startAt е задължителен.")
    LocalDateTime startAt,

    @NotNull(message = "endAt е задължителен.")
    LocalDateTime endAt,

    @NotBlank(message = "Мястото е задължително.")
    String location
) {}
