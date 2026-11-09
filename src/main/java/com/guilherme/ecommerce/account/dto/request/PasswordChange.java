package com.guilherme.ecommerce.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChange(
    @NotBlank String currentPassword, @NotBlank @Size(min = 12, max = 72) String newPassword) {}
