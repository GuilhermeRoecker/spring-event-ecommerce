package com.guilherme.ecommerce.checkout.dto.request;

import com.guilherme.ecommerce.order.domain.Method;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record Checkout(
    @NotNull @Positive Long addressId, @NotNull Method method, @Size(max = 40) String coupon) {}
