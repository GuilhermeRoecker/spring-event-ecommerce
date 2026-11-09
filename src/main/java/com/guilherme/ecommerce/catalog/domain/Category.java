package com.guilherme.ecommerce.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "category")
public class Category {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String name;

  protected Category() {}

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Category(String name) {
    this.name = name;
  }

  public void rename(String name) {
    this.name = name;
  }
}
