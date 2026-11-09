package com.guilherme.ecommerce.order.service;

import static com.guilherme.ecommerce.shared.Resources.require;

import com.guilherme.ecommerce.account.security.CurrentAccount;
import com.guilherme.ecommerce.order.dto.response.DeliveryResponse;
import com.guilherme.ecommerce.order.dto.response.OrderDetailResponse;
import com.guilherme.ecommerce.order.dto.response.OrderItemResponse;
import com.guilherme.ecommerce.order.dto.response.OrderResponse;
import com.guilherme.ecommerce.order.dto.response.OrderSummaryResponse;
import com.guilherme.ecommerce.order.dto.response.PaymentResponse;
import com.guilherme.ecommerce.order.dto.response.SaleResponse;
import com.guilherme.ecommerce.order.repository.DeliveryRepository;
import com.guilherme.ecommerce.order.repository.OrderItemRepository;
import com.guilherme.ecommerce.order.repository.OrderRepository;
import com.guilherme.ecommerce.order.repository.PaymentRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class OrderService {
  private final CurrentAccount current;
  private final OrderRepository orders;
  private final OrderItemRepository items;
  private final PaymentRepository payments;
  private final DeliveryRepository deliveries;

  public OrderService(
      CurrentAccount current,
      OrderRepository orders,
      OrderItemRepository items,
      PaymentRepository payments,
      DeliveryRepository deliveries) {
    this.current = current;
    this.orders = orders;
    this.items = items;
    this.payments = payments;
    this.deliveries = deliveries;
  }

  public List<OrderResponse> orders(int page) {
    return orders
        .findByAccountIdOrderByIdDesc(current.get().id(), PageRequest.of(page, 50))
        .stream()
        .map(
            o ->
                new OrderResponse(
                    o.getId(),
                    o.getCreatedAt(),
                    o.getStatus().name(),
                    o.getSubtotal(),
                    o.getDiscount(),
                    o.getTotal()))
        .toList();
  }

  public OrderDetailResponse order(long id) {
    var o = require(orders.findByIdAndAccountId(id, current.get().id()));
    var p = require(payments.findById(id));
    var d = require(deliveries.findById(id));
    return new OrderDetailResponse(
        new OrderSummaryResponse(
            o.getId(),
            o.getCreatedAt(),
            o.getStatus().name(),
            o.getSubtotal(),
            o.getDiscount(),
            o.getTotal(),
            o.getShippingAddress()),
        items.findByOrderIdOrderByIdAsc(id).stream()
            .map(
                i ->
                    new OrderItemResponse(
                        i.getProduct().getId(), i.getName(), i.getQuantity(), i.getUnitPrice()))
            .toList(),
        new PaymentResponse(p.getMethod().name(), p.getStatus()),
        new DeliveryResponse(d.getStatus(), d.getCarrier(), d.getTrackingCode()));
  }

  public List<SaleResponse> sales(long companyId, int page) {
    current.manage(companyId, false);
    return items.findByCompanyIdOrderByIdDesc(companyId, PageRequest.of(page, 50)).stream()
        .map(
            i ->
                new SaleResponse(
                    i.getOrder().getId(),
                    i.getProduct().getId(),
                    i.getName(),
                    i.getQuantity(),
                    i.getUnitPrice(),
                    i.getOrder().getCreatedAt(),
                    i.getOrder().getStatus().name()))
        .toList();
  }
}
