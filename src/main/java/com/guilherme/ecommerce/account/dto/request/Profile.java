package com.guilherme.ecommerce.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Profile(@NotBlank @Size(max = 160) String name) {}
