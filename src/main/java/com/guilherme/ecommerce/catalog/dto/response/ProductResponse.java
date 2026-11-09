package com.guilherme.ecommerce.catalog.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record ProductResponse(
    long id,
    @JsonProperty("company_id") long companyId,
    @JsonProperty("category_id") long categoryId,
    String name,
    String description,
    BigDecimal price,
    int stock,
    @JsonProperty("image_url") String imageUrl) {}
