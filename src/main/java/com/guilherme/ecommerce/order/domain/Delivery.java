package com.guilherme.ecommerce.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "delivery")
public class Delivery {
  @Id
  @Column(name = "order_id")
  private Long orderId;

  @MapsId
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id")
  private PurchaseOrder order;

  protected Delivery() {}

  @Column(nullable = false, length = 20)
  private String status;

  @Column(length = 120)
  private String carrier;

  @Column(length = 120)
  private String trackingCode;

  public String getStatus() {
    return status;
  }

  public String getCarrier() {
    return carrier;
  }

  public String getTrackingCode() {
    return trackingCode;
  }

  public Delivery(PurchaseOrder order) {
    this.order = order;
    this.status = "PENDING";
  }

  public void track(String status, String carrier, String trackingCode) {
    this.status = status;
    this.carrier = carrier;
    this.trackingCode = trackingCode;
  }
}
