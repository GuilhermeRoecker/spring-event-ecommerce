package com.guilherme.ecommerce.order.domain;

import com.guilherme.ecommerce.account.domain.Company;
import com.guilherme.ecommerce.catalog.domain.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "order_item")
public class OrderItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private PurchaseOrder order;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;

  @Column(nullable = false, length = 160)
  private String name;

  @Column(nullable = false)
  private int quantity;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal unitPrice;

  protected OrderItem() {}

  public Long getId() {
    return id;
  }

  public PurchaseOrder getOrder() {
    return order;
  }

  public Product getProduct() {
    return product;
  }

  public Company getCompany() {
    return company;
  }

  public String getName() {
    return name;
  }

  public int getQuantity() {
    return quantity;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public OrderItem(PurchaseOrder order, Product product, int quantity) {
    this.order = order;
    this.product = product;
    this.company = product.getCompany();
    this.name = product.getName();
    this.quantity = quantity;
    this.unitPrice = product.getPrice();
  }
}
