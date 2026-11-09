package com.guilherme.ecommerce.commerce.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record CartItemResponse(
    @JsonProperty("product_id") long productId,
    String name,
    BigDecimal price,
    int stock,
    boolean active,
    int quantity) {}
