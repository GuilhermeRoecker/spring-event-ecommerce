package com.guilherme.ecommerce.commerce.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record Quantity(@Min(1) @Max(999) int quantity) {}
