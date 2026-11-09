package com.guilherme.ecommerce.order.controller;

import com.guilherme.ecommerce.order.dto.request.Tracking;
import com.guilherme.ecommerce.order.dto.response.SaleResponse;
import com.guilherme.ecommerce.order.service.OrderOperationsService;
import com.guilherme.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OrderOperationsController {
  private final OrderOperationsService operations;
  private final OrderService orders;

  public OrderOperationsController(OrderOperationsService operations, OrderService orders) {
    this.operations = operations;
    this.orders = orders;
  }

  @GetMapping("/manage/order-items")
  public List<SaleResponse> sales(
      @RequestParam long companyId,
      @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page) {
    return orders.sales(companyId, page);
  }

  @PostMapping("/admin/orders/{id}/confirm-payment")
  public void confirm(@PathVariable long id) {
    operations.confirm(id);
  }

  @PutMapping("/admin/orders/{id}/delivery")
  public void tracking(@PathVariable long id, @Valid @RequestBody Tracking r) {
    operations.tracking(id, r);
  }
}
