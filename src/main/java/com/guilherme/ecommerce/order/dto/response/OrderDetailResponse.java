package com.guilherme.ecommerce.order.dto.response;

import java.util.List;

public record OrderDetailResponse(
    OrderSummaryResponse order,
    List<OrderItemResponse> items,
    PaymentResponse payment,
    DeliveryResponse delivery) {}
