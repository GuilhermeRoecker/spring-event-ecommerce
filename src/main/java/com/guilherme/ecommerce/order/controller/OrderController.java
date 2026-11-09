package com.guilherme.ecommerce.order.controller;

import com.guilherme.ecommerce.order.dto.response.OrderDetailResponse;
import com.guilherme.ecommerce.order.dto.response.OrderResponse;
import com.guilherme.ecommerce.order.service.OrderService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OrderController {
  private final OrderService orders;

  public OrderController(OrderService orders) {
    this.orders = orders;
  }

  @GetMapping("/me/orders")
  public List<OrderResponse> orders(
      @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page) {
    return orders.orders(page);
  }

  @GetMapping("/me/orders/{id}")
  public OrderDetailResponse order(@PathVariable long id) {
    return orders.order(id);
  }
}
