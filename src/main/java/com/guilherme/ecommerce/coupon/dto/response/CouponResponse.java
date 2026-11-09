package com.guilherme.ecommerce.coupon.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public record CouponResponse(
    long id,
    String code,
    int percentage,
    @JsonProperty("expires_on") LocalDate expiresOn,
    @JsonProperty("usage_limit") int usageLimit,
    int used,
    boolean active) {}
