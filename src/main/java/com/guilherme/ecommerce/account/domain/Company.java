package com.guilherme.ecommerce.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "company")
public class Company {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 160)
  private String name;

  @Column(nullable = false, length = 200)
  private String legalName;

  protected Company() {}

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getLegalName() {
    return legalName;
  }

  public Company(String name, String legalName) {
    rename(name, legalName);
  }

  public void rename(String name, String legalName) {
    this.name = name;
    this.legalName = legalName;
  }
}
