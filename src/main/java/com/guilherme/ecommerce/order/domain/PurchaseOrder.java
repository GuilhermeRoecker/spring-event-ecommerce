package com.guilherme.ecommerce.order.domain;

import com.guilherme.ecommerce.account.domain.Account;
import com.guilherme.ecommerce.coupon.domain.Coupon;
import com.guilherme.ecommerce.shared.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "purchase_order")
public class PurchaseOrder {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @Column(nullable = false, length = 80)
  private String requestKey;

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private OrderStatus status;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal subtotal;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal discount;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal total;

  @Column(nullable = false, length = 1000)
  private String shippingAddress;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "coupon_id")
  private Coupon coupon;

  protected PurchaseOrder() {}

  public Long getId() {
    return id;
  }

  public Account getAccount() {
    return account;
  }

  public String getRequestKey() {
    return requestKey;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public BigDecimal getSubtotal() {
    return subtotal;
  }

  public BigDecimal getDiscount() {
    return discount;
  }

  public BigDecimal getTotal() {
    return total;
  }

  public String getShippingAddress() {
    return shippingAddress;
  }

  public Coupon getCoupon() {
    return coupon;
  }

  public PurchaseOrder(
      Account account,
      String key,
      BigDecimal subtotal,
      BigDecimal discount,
      String shippingAddress,
      Coupon coupon) {
    this.account = account;
    this.requestKey = key;
    this.subtotal = subtotal;
    this.discount = discount;
    this.total = subtotal.subtract(discount);
    this.shippingAddress = shippingAddress;
    this.coupon = coupon;
    this.status = OrderStatus.AWAITING_PAYMENT;
    this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  public void confirmPayment() {
    if (status != OrderStatus.AWAITING_PAYMENT) throw conflict("Pedido não aguarda pagamento");
    status = OrderStatus.PAID;
  }

  public void ship() {
    if (status != OrderStatus.PAID) throw conflict("Transição de entrega inválida");
    status = OrderStatus.SHIPPED;
  }

  public void deliver() {
    if (status != OrderStatus.SHIPPED) throw conflict("Transição de entrega inválida");
    status = OrderStatus.DELIVERED;
  }

  private BusinessException conflict(String reason) {
    return BusinessException.conflict(reason);
  }
}
