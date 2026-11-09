package com.guilherme.ecommerce.catalog.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductInput(
    @NotNull @Positive Long companyId,
    @NotNull @Positive Long categoryId,
    @NotBlank @Size(max = 160) String name,
    @NotNull @Size(max = 2000) String description,
    @NotNull @DecimalMin("0.01") @DecimalMax("999999999999.99") @Digits(integer = 12, fraction = 2)
        BigDecimal price,
    @Min(0) @Max(1000000) int stock,
    boolean active,
    @Size(max = 1000) @Pattern(regexp = "https://[^\\s]+|^$", message = "Use HTTPS")
        String imageUrl) {}
