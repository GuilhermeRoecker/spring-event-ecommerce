package com.guilherme.ecommerce.commerce.domain;

public class CartItemId implements java.io.Serializable {
  private static final long serialVersionUID = 1L;
  public Long account;
  public Long product;

  public CartItemId() {}

  public CartItemId(Long a, Long b) {
    this.account = a;
    this.product = b;
  }

  public boolean equals(Object other) {
    return other instanceof CartItemId o
        && java.util.Objects.equals(account, o.account)
        && java.util.Objects.equals(product, o.product);
  }

  public int hashCode() {
    return java.util.Objects.hash(account, product);
  }
}
