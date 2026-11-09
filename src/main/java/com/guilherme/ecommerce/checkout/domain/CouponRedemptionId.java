package com.guilherme.ecommerce.checkout.domain;

public class CouponRedemptionId implements java.io.Serializable {
  private static final long serialVersionUID = 1L;
  public Long coupon;
  public Long account;

  public CouponRedemptionId() {}

  public CouponRedemptionId(Long a, Long b) {
    this.coupon = a;
    this.account = b;
  }

  public boolean equals(Object other) {
    return other instanceof CouponRedemptionId o
        && java.util.Objects.equals(coupon, o.coupon)
        && java.util.Objects.equals(account, o.account);
  }

  public int hashCode() {
    return java.util.Objects.hash(coupon, account);
  }
}
