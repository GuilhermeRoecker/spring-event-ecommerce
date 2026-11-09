package com.guilherme.ecommerce.catalog.domain;

import com.guilherme.ecommerce.account.domain.Company;
import com.guilherme.ecommerce.shared.exception.BusinessException;
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
@Table(name = "product")
public class Product {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  private Category category;

  @Column(nullable = false, length = 160)
  private String name;

  @Column(nullable = false, length = 2000)
  private String description;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal price;

  @Column(nullable = false)
  private int stock;

  @Column(nullable = false)
  private boolean active;

  @Column(nullable = true, length = 1000)
  private String imageUrl;

  protected Product() {}

  public Long getId() {
    return id;
  }

  public Company getCompany() {
    return company;
  }

  public Category getCategory() {
    return category;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public int getStock() {
    return stock;
  }

  public boolean getActive() {
    return active;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public Product(
      Company company,
      Category category,
      String name,
      String description,
      BigDecimal price,
      int stock,
      boolean active,
      String imageUrl) {
    this.company = company;
    revise(category, name, description, price, stock, active, imageUrl);
  }

  public void revise(
      Category category,
      String name,
      String description,
      BigDecimal price,
      int stock,
      boolean active,
      String imageUrl) {
    this.category = category;
    this.name = name;
    this.description = description;
    this.price = price;
    this.stock = stock;
    this.active = active;
    this.imageUrl = imageUrl;
  }

  public void deactivate() {
    active = false;
  }

  public void purchase(int quantity) {
    if (quantity < 1 || !active || stock < quantity)
      throw BusinessException.conflict("Produto indisponível ou estoque insuficiente");
    stock -= quantity;
  }
}
