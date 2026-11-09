package com.guilherme.ecommerce.order.dto.request;

import com.guilherme.ecommerce.order.domain.DeliveryStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record Tracking(
    @NotNull DeliveryStatus status,
    @NotBlank @Size(max = 120) String carrier,
    @NotBlank @Size(max = 120) String trackingCode) {}
