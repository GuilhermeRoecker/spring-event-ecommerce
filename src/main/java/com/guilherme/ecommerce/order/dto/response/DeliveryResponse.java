package com.guilherme.ecommerce.order.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DeliveryResponse(
    String status, String carrier, @JsonProperty("tracking_code") String trackingCode) {}
