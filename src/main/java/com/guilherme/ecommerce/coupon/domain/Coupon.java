package com.guilherme.ecommerce.coupon.domain;

import com.guilherme.ecommerce.shared.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "coupon")
public class Coupon {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 40)
  private String code;

  @Column(nullable = false)
  private int percentage;

  @Column(nullable = false)
  private LocalDate expiresOn;

  @Column(nullable = false)
  private int usageLimit;

  @Column(nullable = false)
  private int used;

  @Column(nullable = false)
  private boolean active;

  protected Coupon() {}

  public Long getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public int getPercentage() {
    return percentage;
  }

  public LocalDate getExpiresOn() {
    return expiresOn;
  }

  public int getUsageLimit() {
    return usageLimit;
  }

  public int getUsed() {
    return used;
  }

  public boolean getActive() {
    return active;
  }

  public Coupon(String code, int percentage, LocalDate expiresOn, int usageLimit) {
    this.code = code;
    this.percentage = percentage;
    this.expiresOn = expiresOn;
    this.usageLimit = usageLimit;
    this.active = true;
  }

  public void redeem(LocalDate today) {
    if (!active || expiresOn.isBefore(today) || used >= usageLimit)
      throw BusinessException.invalid("Cupom indisponível");
    used++;
  }

  public BigDecimal discount(BigDecimal subtotal) {
    return subtotal
        .multiply(BigDecimal.valueOf(percentage))
        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
  }

  public void deactivate() {
    active = false;
  }
}
