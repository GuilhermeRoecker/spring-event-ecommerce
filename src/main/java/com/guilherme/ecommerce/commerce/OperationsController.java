package com.guilherme.ecommerce.commerce;

import com.guilherme.ecommerce.auth.CurrentAccount;
import com.guilherme.ecommerce.shared.Database;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class OperationsController {
  private final Database db;
  private final CurrentAccount current;

  public OperationsController(Database db, CurrentAccount current) {
    this.db = db;
    this.current = current;
  }

  public record Coupon(
      @NotBlank @Pattern(regexp = "[A-Z0-9_-]{3,40}") String code,
      @Min(1) @Max(100) int percentage,
      @NotNull @FutureOrPresent LocalDate expiresOn,
      @Min(1) int usageLimit) {}

  @GetMapping("/admin/coupons")
  public Object coupons() {
    current.admin();
    return db.list("SELECT * FROM coupon ORDER BY id LIMIT 500");
  }

  @PostMapping("/admin/coupons")
  @ResponseStatus(HttpStatus.CREATED)
  public void coupon(@Valid @RequestBody Coupon r) {
    current.admin();
    db.update(
        "INSERT INTO coupon(code,percentage,expires_on,usage_limit) VALUES(?,?,?,?)",
        r.code(),
        r.percentage(),
        r.expiresOn(),
        r.usageLimit());
  }

  @DeleteMapping("/admin/coupons/{id}")
  public void disable(@PathVariable long id) {
    current.admin();
    db.one("SELECT id FROM coupon WHERE id=?", id);
    db.update("UPDATE coupon SET active=FALSE WHERE id=?", id);
  }

  @GetMapping("/manage/order-items")
  public Object sales(
      @RequestParam long companyId,
      @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page) {
    current.manage(companyId, false);
    return db.list(
        "SELECT i.order_id,i.product_id,i.name,i.quantity,i.unit_price,o.created_at,o.status FROM"
            + " order_item i JOIN purchase_order o ON o.id=i.order_id WHERE i.company_id=? ORDER BY"
            + " i.id DESC LIMIT 50 OFFSET ?",
        companyId,
        page * 50);
  }

  @PostMapping("/admin/orders/{id}/confirm-payment")
  @Transactional
  public void confirm(@PathVariable long id) {
    current.admin();
    var o = db.one("SELECT status FROM purchase_order WHERE id=? FOR UPDATE", id);
    if (!o.get("status").equals("AWAITING_PAYMENT"))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Pedido não aguarda pagamento");
    db.update("UPDATE payment SET status='PAID' WHERE order_id=?", id);
    db.update("UPDATE purchase_order SET status='PAID' WHERE id=?", id);
  }

  public enum DeliveryStatus {
    SHIPPED,
    DELIVERED
  }

  public record Tracking(
      @NotNull DeliveryStatus status,
      @NotBlank @Size(max = 120) String carrier,
      @NotBlank @Size(max = 120) String trackingCode) {}

  @PutMapping("/admin/orders/{id}/delivery")
  @Transactional
  public void tracking(@PathVariable long id, @Valid @RequestBody Tracking r) {
    current.admin();
    var o = db.one("SELECT status FROM purchase_order WHERE id=? FOR UPDATE", id);
    String previous = (String) o.get("status");
    if (!(previous.equals("PAID") && r.status() == DeliveryStatus.SHIPPED
        || previous.equals("SHIPPED") && r.status() == DeliveryStatus.DELIVERED))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Transição de entrega inválida");
    db.update(
        "UPDATE delivery SET status=?,carrier=?,tracking_code=? WHERE order_id=?",
        r.status().name(),
        r.carrier(),
        r.trackingCode(),
        id);
    db.update("UPDATE purchase_order SET status=? WHERE id=?", r.status().name(), id);
  }
}
