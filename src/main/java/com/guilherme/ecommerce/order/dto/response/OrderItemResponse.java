package com.guilherme.ecommerce.order.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record OrderItemResponse(
    @JsonProperty("product_id") long productId,
    String name,
    int quantity,
    @JsonProperty("unit_price") BigDecimal unitPrice) {}
