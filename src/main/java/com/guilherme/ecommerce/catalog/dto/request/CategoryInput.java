package com.guilherme.ecommerce.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryInput(@NotBlank @Size(max = 120) String name) {}
