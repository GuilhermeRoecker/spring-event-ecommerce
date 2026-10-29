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
  private final CheckoutRepository repository;
  private final CurrentAccount current;

  public CheckoutService(CheckoutRepository repository, CurrentAccount current) {
    this.repository = repository;
    this.current = current;
  }

  public void lockAccount(long id) {
    repository.lockAccount(id);
  }

  @Transactional
  public Map<String, Object> checkout(CommerceDtos.Checkout r, String key) {
    long user = current.get().id();
    // Serialize cart changes and retries for this account in the same transaction.
    lockAccount(user);
    var previous = repository.findPreviousOrders(user, key);
    if (!previous.isEmpty()) return previous.getFirst();
    var address = repository.findOwnedAddress(r.addressId(), user);
    var items = repository.findCartItems(user);
    if (items.isEmpty()) throw bad("Carrinho vazio");
    BigDecimal subtotal = BigDecimal.ZERO;
    List<Map<String, Object>> products = new ArrayList<>();
    // Every checkout acquires product locks in ID order to avoid lock-order inversion.
    for (var item : items) {
      var p = repository.lockProduct(Database.id(item, "product_id"));
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
      var c = repository.lockCoupon(r.coupon());
      couponId = Database.id(c, "id");
      LocalDate expires = ((java.sql.Date) c.get("expires_on")).toLocalDate();
      if (!Boolean.TRUE.equals(c.get("active"))
          || expires.isBefore(LocalDate.now())
          || Database.id(c, "used") >= Database.id(c, "usage_limit"))
        throw bad("Cupom indisponível");
      if (!repository.findRedemptions(couponId, user).isEmpty())
        throw bad("Cupom já utilizado por esta conta");
      discount =
          subtotal
              .multiply(BigDecimal.valueOf(Database.id(c, "percentage")))
              .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
      repository.incrementCouponUsage(couponId);
    }
    BigDecimal total = subtotal.subtract(discount);
    String shipping =
        String.join(
            ", ",
            address.values().stream().filter(Objects::nonNull).map(Object::toString).toList());
    repository.insertOrder(user, key, subtotal, discount, total, shipping, couponId);
    var order = repository.findCreatedOrder(user, key);
    long orderId = Database.id(order, "id");
    for (int i = 0; i < items.size(); i++) {
      var item = items.get(i);
      var p = products.get(i);
      repository.insertOrderItem(
          orderId,
          Database.id(p, "id"),
          Database.id(p, "company_id"),
          (String) p.get("name"),
          ((Number) item.get("quantity")).intValue(),
          (BigDecimal) p.get("price"));
      repository.decrementStock(((Number) item.get("quantity")).intValue(), Database.id(p, "id"));
    }
    repository.insertPayment(orderId, r.method().name());
    repository.insertDelivery(orderId);
    if (couponId != null) repository.insertRedemption(couponId, user, orderId);
    repository.clearCart(user);
    return order;
  }

  private ResponseStatusException bad(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}
