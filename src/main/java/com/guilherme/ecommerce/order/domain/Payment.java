package com.guilherme.ecommerce.order.domain;

import com.guilherme.ecommerce.shared.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment")
public class Payment {
  @Id
  @Column(name = "order_id")
  private Long orderId;

  @MapsId
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id")
  private PurchaseOrder order;

  protected Payment() {}

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Method method;

  @Column(nullable = false, length = 20)
  private String status;

  public Method getMethod() {
    return method;
  }

  public String getStatus() {
    return status;
  }

  public Payment(PurchaseOrder order, Method method) {
    this.order = order;
    this.method = method;
    this.status = "PENDING";
  }

  public void confirm() {
    if (!status.equals("PENDING")) throw BusinessException.conflict("Pagamento não está pendente");
    status = "PAID";
  }
}
