package com.guilherme.ecommerce.order.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SaleResponse(
    @JsonProperty("order_id") long orderId,
    @JsonProperty("product_id") long productId,
    String name,
    int quantity,
    @JsonProperty("unit_price") BigDecimal unitPrice,
    @JsonProperty("created_at") OffsetDateTime createdAt,
    String status) {}
