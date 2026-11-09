package com.guilherme.ecommerce.order.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OrderSummaryResponse(
    long id,
    @JsonProperty("created_at") OffsetDateTime createdAt,
    String status,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    @JsonProperty("shipping_address") String shippingAddress) {}
