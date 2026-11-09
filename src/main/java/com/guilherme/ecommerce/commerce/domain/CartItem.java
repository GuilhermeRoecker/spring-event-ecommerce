package com.guilherme.ecommerce.commerce.domain;

import com.guilherme.ecommerce.account.domain.Account;
import com.guilherme.ecommerce.catalog.domain.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "cart_item")
@IdClass(CartItemId.class)
public class CartItem {
  @Id
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  public Account getAccount() {
    return account;
  }

  @Id
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  public Product getProduct() {
    return product;
  }

  protected CartItem() {}

  @Column(nullable = false)
  private int quantity;

  public CartItem(Account account, Product product, int quantity) {
    this.account = account;
    this.product = product;
    this.quantity = quantity;
  }

  public int getQuantity() {
    return quantity;
  }

  public void changeQuantity(int quantity) {
    this.quantity = quantity;
  }
}
