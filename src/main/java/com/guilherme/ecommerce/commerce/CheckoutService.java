package com.guilherme.ecommerce.commerce;

import com.guilherme.ecommerce.auth.CurrentAccount;
import com.guilherme.ecommerce.shared.Database;
import java.math.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CheckoutService {
  private final Database db;
  private final CurrentAccount current;

  public CheckoutService(Database db, CurrentAccount current) {
    this.db = db;
    this.current = current;
  }

  public void lockAccount(long id) {
    db.one("SELECT id FROM account WHERE id=? FOR UPDATE", id);
  }

  @Transactional
  public Map<String, Object> checkout(CommerceController.Checkout r, String key) {
    long user = current.get().id();
    // Serialize cart changes and retries for this account in the same transaction.
    lockAccount(user);
    var previous =
        db.list(
            "SELECT id,total,status FROM purchase_order WHERE account_id=? AND request_key=?",
            user,
            key);
    if (!previous.isEmpty()) return previous.getFirst();
    var address =
        db.one(
            "SELECT street,number,complement,city,state,postal_code FROM address WHERE id=? AND"
                + " account_id=?",
            r.addressId(),
            user);
    var items =
        db.list(
            "SELECT product_id,quantity FROM cart_item WHERE account_id=? ORDER BY product_id",
            user);
    if (items.isEmpty()) throw bad("Carrinho vazio");
    BigDecimal subtotal = BigDecimal.ZERO;
    List<Map<String, Object>> products = new ArrayList<>();
    // Every checkout acquires product locks in ID order to avoid lock-order inversion.
    for (var item : items) {
      var p =
          db.one(
              "SELECT id,company_id,name,price,stock,active FROM product WHERE id=? FOR UPDATE",
              item.get("product_id"));
      int quantity = ((Number) item.get("quantity")).intValue();
      if (!Boolean.TRUE.equals(p.get("active")) || ((Number) p.get("stock")).intValue() < quantity)
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "Produto indisponível ou estoque insuficiente");
      subtotal = subtotal.add(((BigDecimal) p.get("price")).multiply(BigDecimal.valueOf(quantity)));
      products.add(p);
    }
    BigDecimal discount = BigDecimal.ZERO;
    Long couponId = null;
    if (r.coupon() != null && !r.coupon().isBlank()) {
      var c = db.one("SELECT * FROM coupon WHERE code=? FOR UPDATE", r.coupon());
      couponId = db.id(c, "id");
      LocalDate expires = ((java.sql.Date) c.get("expires_on")).toLocalDate();
      if (!Boolean.TRUE.equals(c.get("active"))
          || expires.isBefore(LocalDate.now())
          || db.id(c, "used") >= db.id(c, "usage_limit")) throw bad("Cupom indisponível");
      if (!db.list(
              "SELECT order_id FROM coupon_redemption WHERE coupon_id=? AND account_id=?",
              couponId,
              user)
          .isEmpty()) throw bad("Cupom já utilizado por esta conta");
      discount =
          subtotal
              .multiply(BigDecimal.valueOf(db.id(c, "percentage")))
              .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
      db.update("UPDATE coupon SET used=used+1 WHERE id=?", couponId);
    }
    BigDecimal total = subtotal.subtract(discount);
    String shipping =
        String.join(
            ", ",
            address.values().stream().filter(Objects::nonNull).map(Object::toString).toList());
    db.update(
        "INSERT INTO"
            + " purchase_order(account_id,request_key,subtotal,discount,total,shipping_address,coupon_id)"
            + " VALUES(?,?,?,?,?,?,?)",
        user,
        key,
        subtotal,
        discount,
        total,
        shipping,
        couponId);
    var order =
        db.one(
            "SELECT id,total,status FROM purchase_order WHERE account_id=? AND request_key=?",
            user,
            key);
    long orderId = db.id(order, "id");
    for (int i = 0; i < items.size(); i++) {
      var item = items.get(i);
      var p = products.get(i);
      db.update(
          "INSERT INTO order_item(order_id,product_id,company_id,name,quantity,unit_price)"
              + " VALUES(?,?,?,?,?,?)",
          orderId,
          p.get("id"),
          p.get("company_id"),
          p.get("name"),
          item.get("quantity"),
          p.get("price"));
      db.update("UPDATE product SET stock=stock-? WHERE id=?", item.get("quantity"), p.get("id"));
    }
    db.update("INSERT INTO payment(order_id,method) VALUES(?,?)", orderId, r.method().name());
    db.update("INSERT INTO delivery(order_id) VALUES(?)", orderId);
    if (couponId != null)
      db.update(
          "INSERT INTO coupon_redemption(coupon_id,account_id,order_id) VALUES(?,?,?)",
          couponId,
          user,
          orderId);
    db.update("DELETE FROM cart_item WHERE account_id=?", user);
    return order;
  }

  private ResponseStatusException bad(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}
