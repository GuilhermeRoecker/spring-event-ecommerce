package com.guilherme.ecommerce.checkout.domain;

import com.guilherme.ecommerce.account.domain.Account;
import com.guilherme.ecommerce.coupon.domain.Coupon;
import com.guilherme.ecommerce.order.domain.PurchaseOrder;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "coupon_redemption")
@IdClass(CouponRedemptionId.class)
public class CouponRedemption {
  @Id
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "coupon_id", nullable = false)
  private Coupon coupon;

  public Coupon getCoupon() {
    return coupon;
  }

  @Id
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  public Account getAccount() {
    return account;
  }

  protected CouponRedemption() {}

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private PurchaseOrder order;

  public CouponRedemption(Coupon coupon, Account account, PurchaseOrder order) {
    this.coupon = coupon;
    this.account = account;
    this.order = order;
  }
}
