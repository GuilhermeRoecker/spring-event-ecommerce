package com.guilherme.ecommerce.commerce;

import com.guilherme.ecommerce.shared.Database;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class OperationsRepository {
  private final Database db;

  public OperationsRepository(Database db) {
    this.db = db;
  }

  public List<Map<String, Object>> findCoupons() {
    return db.list("SELECT * FROM coupon ORDER BY id LIMIT 500");
  }

  public int insertCoupon(String code, int percentage, LocalDate expiresOn, int usageLimit) {
    return db.update(
        "INSERT INTO coupon(code,percentage,expires_on,usage_limit) VALUES(?,?,?,?)",
        code,
        percentage,
        expiresOn,
        usageLimit);
  }

  public Map<String, Object> requireCoupon(long id) {
    return db.one("SELECT id FROM coupon WHERE id=?", id);
  }

  public int deactivateCoupon(long id) {
    return db.update("UPDATE coupon SET active=FALSE WHERE id=?", id);
  }

  public List<Map<String, Object>> findSales(long companyId, int offset) {
    return db.list(
        "SELECT i.order_id,i.product_id,i.name,i.quantity,i.unit_price,o.created_at,o.status FROM"
            + " order_item i JOIN purchase_order o ON o.id=i.order_id WHERE i.company_id=? ORDER BY"
            + " i.id DESC LIMIT 50 OFFSET ?",
        companyId,
        offset);
  }

  public Map<String, Object> lockOrderForPayment(long id) {
    return db.one("SELECT status FROM purchase_order WHERE id=? FOR UPDATE", id);
  }

  public int markPaymentPaid(long id) {
    return db.update("UPDATE payment SET status='PAID' WHERE order_id=?", id);
  }

  public int markOrderPaid(long id) {
    return db.update("UPDATE purchase_order SET status='PAID' WHERE id=?", id);
  }

  public Map<String, Object> lockOrderForDelivery(long id) {
    return db.one("SELECT status FROM purchase_order WHERE id=? FOR UPDATE", id);
  }

  public int updateDelivery(String status, String carrier, String trackingCode, long orderId) {
    return db.update(
        "UPDATE delivery SET status=?,carrier=?,tracking_code=? WHERE order_id=?",
        status,
        carrier,
        trackingCode,
        orderId);
  }

  public int updateOrderStatus(String status, long id) {
    return db.update("UPDATE purchase_order SET status=? WHERE id=?", status, id);
  }
}
