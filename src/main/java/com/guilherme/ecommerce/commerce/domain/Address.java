package com.guilherme.ecommerce.commerce.domain;

import com.guilherme.ecommerce.account.domain.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "address")
public class Address {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @Column(nullable = false, length = 200)
  private String street;

  @Column(nullable = false, length = 30)
  private String number;

  @Column(nullable = true, length = 120)
  private String complement;

  @Column(nullable = false, length = 120)
  private String city;

  @Column(nullable = false, length = 2)
  private String state;

  @Column(nullable = false, length = 8)
  private String postalCode;

  protected Address() {}

  public Long getId() {
    return id;
  }

  public Account getAccount() {
    return account;
  }

  public String getStreet() {
    return street;
  }

  public String getNumber() {
    return number;
  }

  public String getComplement() {
    return complement;
  }

  public String getCity() {
    return city;
  }

  public String getState() {
    return state;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public Address(
      Account account,
      String street,
      String number,
      String complement,
      String city,
      String state,
      String postalCode) {
    this.account = account;
    revise(street, number, complement, city, state, postalCode);
  }

  public void revise(
      String street,
      String number,
      String complement,
      String city,
      String state,
      String postalCode) {
    this.street = street;
    this.number = number;
    this.complement = complement;
    this.city = city;
    this.state = state;
    this.postalCode = postalCode;
  }

  public String snapshot() {
    return java.util.stream.Stream.of(street, number, complement, city, state, postalCode)
        .filter(java.util.Objects::nonNull)
        .collect(java.util.stream.Collectors.joining(", "));
  }
}
