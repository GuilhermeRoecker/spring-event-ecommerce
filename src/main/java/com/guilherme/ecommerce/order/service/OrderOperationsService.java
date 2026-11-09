package com.guilherme.ecommerce.order.service;

import static com.guilherme.ecommerce.shared.Resources.require;

import com.guilherme.ecommerce.account.security.CurrentAccount;
import com.guilherme.ecommerce.order.domain.DeliveryStatus;
import com.guilherme.ecommerce.order.dto.request.Tracking;
import com.guilherme.ecommerce.order.repository.DeliveryRepository;
import com.guilherme.ecommerce.order.repository.OrderRepository;
import com.guilherme.ecommerce.order.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class OrderOperationsService {
  private final CurrentAccount current;
  private final OrderRepository orders;
  private final PaymentRepository payments;
  private final DeliveryRepository deliveries;

  public OrderOperationsService(
      CurrentAccount current,
      OrderRepository orders,
      PaymentRepository payments,
      DeliveryRepository deliveries) {
    this.current = current;
    this.orders = orders;
    this.payments = payments;
    this.deliveries = deliveries;
  }

  @Transactional
  public void confirm(long id) {
    current.admin();
    var o = require(orders.lockById(id));
    o.confirmPayment();
    require(payments.findById(id)).confirm();
  }

  @Transactional
  public void tracking(long id, @jakarta.validation.Valid Tracking r) {
    current.admin();
    var o = require(orders.lockById(id));
    if (r.status() == DeliveryStatus.SHIPPED) o.ship();
    else o.deliver();
    require(deliveries.findById(id)).track(r.status().name(), r.carrier(), r.trackingCode());
  }
}
