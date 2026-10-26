package com.guilherme.ecommerce.commerce;

import com.guilherme.ecommerce.auth.CurrentAccount;
import com.guilherme.ecommerce.shared.Database;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CommerceController {
  private final Database db;
  private final CurrentAccount current;
  private final CheckoutService checkout;

  public CommerceController(Database db, CurrentAccount current, CheckoutService checkout) {
    this.db = db;
    this.current = current;
    this.checkout = checkout;
  }

  public record AddressInput(
      @NotBlank @Size(max = 200) String street,
      @NotBlank @Size(max = 30) String number,
      @Size(max = 120) String complement,
      @NotBlank @Size(max = 120) String city,
      @NotBlank
          @Pattern(
              regexp =
                  "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO")
          String state,
      @NotBlank @Pattern(regexp = "[0-9]{8}") String postalCode) {}

  @GetMapping("/me/addresses")
  public Object addresses() {
    return db.list(
        "SELECT id,street,number,complement,city,state,postal_code FROM address WHERE account_id=?"
            + " ORDER BY id LIMIT 100",
        current.get().id());
  }

  @PostMapping("/me/addresses")
  @ResponseStatus(HttpStatus.CREATED)
  public void address(@Valid @RequestBody AddressInput r) {
    db.update(
        "INSERT INTO address(account_id,street,number,complement,city,state,postal_code)"
            + " VALUES(?,?,?,?,?,?,?)",
        current.get().id(),
        r.street(),
        r.number(),
        r.complement(),
        r.city(),
        r.state(),
        r.postalCode());
  }

  @PutMapping("/me/addresses/{id}")
  @Transactional
  public void editAddress(@PathVariable long id, @Valid @RequestBody AddressInput r) {
    long u = current.get().id();
    checkout.lockAccount(u);
    db.one("SELECT id FROM address WHERE id=? AND account_id=?", id, u);
    db.update(
        "UPDATE address SET street=?,number=?,complement=?,city=?,state=?,postal_code=? WHERE id=?"
            + " AND account_id=?",
        r.street(),
        r.number(),
        r.complement(),
        r.city(),
        r.state(),
        r.postalCode(),
        id,
        u);
  }

  @DeleteMapping("/me/addresses/{id}")
  @Transactional
  public void deleteAddress(@PathVariable long id) {
    long u = current.get().id();
    checkout.lockAccount(u);
    db.one("SELECT id FROM address WHERE id=? AND account_id=?", id, u);
    db.update("DELETE FROM address WHERE id=? AND account_id=?", id, u);
  }

  public record Quantity(@Min(1) @Max(999) int quantity) {}

  @GetMapping("/me/cart")
  public Object cart() {
    return db.list(
        "SELECT p.id AS product_id,p.name,p.price,p.stock,p.active,c.quantity FROM cart_item c JOIN"
            + " product p ON p.id=c.product_id WHERE c.account_id=? ORDER BY p.id",
        current.get().id());
  }

  @PutMapping("/me/cart/{productId}")
  @Transactional
  public void item(@PathVariable long productId, @Valid @RequestBody Quantity r) {
    long user = current.get().id();
    checkout.lockAccount(user);
    var p = db.one("SELECT stock FROM product WHERE id=? AND active=TRUE", productId);
    if (((Number) p.get("stock")).intValue() < r.quantity())
      throw new org.springframework.web.server.ResponseStatusException(
          HttpStatus.CONFLICT, "Estoque insuficiente");
    db.update("DELETE FROM cart_item WHERE account_id=? AND product_id=?", user, productId);
    db.update(
        "INSERT INTO cart_item(account_id,product_id,quantity) VALUES(?,?,?)",
        user,
        productId,
        r.quantity());
  }

  @DeleteMapping("/me/cart/{productId}")
  @Transactional
  public void remove(@PathVariable long productId) {
    long user = current.get().id();
    checkout.lockAccount(user);
    db.update("DELETE FROM cart_item WHERE account_id=? AND product_id=?", user, productId);
  }

  public enum Method {
    PIX,
    CARD,
    BOLETO
  }

  public record Checkout(
      @NotNull @Positive Long addressId, @NotNull Method method, @Size(max = 40) String coupon) {}

  @PostMapping("/me/checkout")
  @ResponseStatus(HttpStatus.CREATED)
  public Object checkout(
      @Valid @RequestBody Checkout r,
      @RequestHeader("Idempotency-Key") @Pattern(regexp = "[a-zA-Z0-9_-]{8,80}") String key) {
    return checkout.checkout(r, key);
  }

  @GetMapping("/me/orders")
  public Object orders(@RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page) {
    return db.list(
        "SELECT id,created_at,status,subtotal,discount,total FROM purchase_order WHERE account_id=?"
            + " ORDER BY id DESC LIMIT 50 OFFSET ?",
        current.get().id(),
        page * 50);
  }

  @GetMapping("/me/orders/{id}")
  public Object order(@PathVariable long id) {
    var order =
        db.one(
            "SELECT id,created_at,status,subtotal,discount,total,shipping_address FROM"
                + " purchase_order WHERE id=? AND account_id=?",
            id,
            current.get().id());
    return Map.of(
        "order",
        order,
        "items",
        db.list(
            "SELECT product_id,name,quantity,unit_price FROM order_item WHERE order_id=? ORDER BY"
                + " id",
            id),
        "payment",
        db.one("SELECT method,status FROM payment WHERE order_id=?", id),
        "delivery",
        db.one("SELECT status,carrier,tracking_code FROM delivery WHERE order_id=?", id));
  }
}
