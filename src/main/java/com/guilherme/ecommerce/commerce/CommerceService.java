package com.guilherme.ecommerce.commerce;

import com.guilherme.ecommerce.auth.CurrentAccount;
import com.guilherme.ecommerce.commerce.CommerceDtos.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommerceService {
  private final CommerceRepository repository;
  private final CurrentAccount current;
  private final CheckoutService checkout;

  public CommerceService(
      CommerceRepository repository, CurrentAccount current, CheckoutService checkout) {
    this.repository = repository;
    this.current = current;
    this.checkout = checkout;
  }

  public Object addresses() {
    return repository.findAddresses(current.get().id());
  }

  public void address(AddressInput r) {
    repository.insertAddress(
        current.get().id(),
        r.street(),
        r.number(),
        r.complement(),
        r.city(),
        r.state(),
        r.postalCode());
  }

  @Transactional
  public void editAddress(long id, AddressInput r) {
    long u = current.get().id();
    checkout.lockAccount(u);
    repository.requireOwnedAddress(id, u);
    repository.updateAddress(
        r.street(), r.number(), r.complement(), r.city(), r.state(), r.postalCode(), id, u);
  }

  @Transactional
  public void deleteAddress(long id) {
    long u = current.get().id();
    checkout.lockAccount(u);
    repository.requireAddressForDeletion(id, u);
    repository.deleteAddress(id, u);
  }

  public Object cart() {
    return repository.findCart(current.get().id());
  }

  @Transactional
  public void item(long productId, Quantity r) {
    long user = current.get().id();
    checkout.lockAccount(user);
    var p = repository.findAvailableStock(productId);
    if (((Number) p.get("stock")).intValue() < r.quantity())
      throw new org.springframework.web.server.ResponseStatusException(
          HttpStatus.CONFLICT, "Estoque insuficiente");
    repository.deleteCartItem(user, productId);
    repository.insertCartItem(user, productId, r.quantity());
  }

  @Transactional
  public void remove(long productId) {
    long user = current.get().id();
    checkout.lockAccount(user);
    repository.removeCartItem(user, productId);
  }

  public Object checkout(Checkout r, String key) {
    return checkout.checkout(r, key);
  }

  public Object orders(int page) {
    return repository.findOrders(current.get().id(), page * 50);
  }

  public Object order(long id) {
    var order = repository.findOwnedOrder(id, current.get().id());
    return Map.of(
        "order",
        order,
        "items",
        repository.findOrderItems(id),
        "payment",
        repository.findPayment(id),
        "delivery",
        repository.findDelivery(id));
  }
}
