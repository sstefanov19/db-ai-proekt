package com.consult.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "Email е задължителен.")
    @Email(message = "Невалиден email.")
    String email,

    @NotBlank(message = "Паролата е задължителна.")
    String password
) {}
