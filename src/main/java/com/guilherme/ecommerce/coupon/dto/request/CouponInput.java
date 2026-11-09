package com.guilherme.ecommerce.coupon.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record CouponInput(
    @NotBlank @Pattern(regexp = "[A-Z0-9_-]{3,40}") String code,
    @Min(1) @Max(100) int percentage,
    @NotNull @FutureOrPresent LocalDate expiresOn,
    @Min(1) int usageLimit) {}
