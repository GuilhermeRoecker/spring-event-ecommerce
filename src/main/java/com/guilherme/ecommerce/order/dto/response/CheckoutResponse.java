package com.guilherme.ecommerce.order.dto.response;

import java.math.BigDecimal;

public record CheckoutResponse(long id, BigDecimal total, String status) {}
