package com.guilherme.ecommerce.commerce;

import com.guilherme.ecommerce.shared.Database;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class CheckoutRepository {
  private final Database db;

  public CheckoutRepository(Database db) {
    this.db = db;
  }

  public Map<String, Object> lockAccount(long id) {
    return db.one("SELECT id FROM account WHERE id=? FOR UPDATE", id);
  }

  public List<Map<String, Object>> findPreviousOrders(long accountId, String requestKey) {
    return db.list(
        "SELECT id,total,status FROM purchase_order WHERE account_id=? AND request_key=?",
        accountId,
        requestKey);
  }

  public Map<String, Object> findOwnedAddress(long addressId, long accountId) {
    return db.one(
        "SELECT street,number,complement,city,state,postal_code FROM address WHERE id=? AND"
            + " account_id=?",
        addressId,
        accountId);
  }

  public List<Map<String, Object>> findCartItems(long accountId) {
    return db.list(
        "SELECT product_id,quantity FROM cart_item WHERE account_id=? ORDER BY product_id",
        accountId);
  }

  public Map<String, Object> lockProduct(long productId) {
    return db.one(
        "SELECT id,company_id,name,price,stock,active FROM product WHERE id=? FOR UPDATE",
        productId);
  }

  public Map<String, Object> lockCoupon(String code) {
    return db.one("SELECT * FROM coupon WHERE code=? FOR UPDATE", code);
  }

  public List<Map<String, Object>> findRedemptions(long couponId, long accountId) {
    return db.list(
        "SELECT order_id FROM coupon_redemption WHERE coupon_id=? AND account_id=?",
        couponId,
        accountId);
  }

  public int incrementCouponUsage(long couponId) {
    return db.update("UPDATE coupon SET used=used+1 WHERE id=?", couponId);
  }

  public int insertOrder(
      long accountId,
      String requestKey,
      BigDecimal subtotal,
      BigDecimal discount,
      BigDecimal total,
      String shippingAddress,
      Long couponId) {
    return db.update(
        "INSERT INTO"
            + " purchase_order(account_id,request_key,subtotal,discount,total,shipping_address,coupon_id)"
            + " VALUES(?,?,?,?,?,?,?)",
        accountId,
        requestKey,
        subtotal,
        discount,
        total,
        shippingAddress,
        couponId);
  }

  public Map<String, Object> findCreatedOrder(long accountId, String requestKey) {
    return db.one(
        "SELECT id,total,status FROM purchase_order WHERE account_id=? AND request_key=?",
        accountId,
        requestKey);
  }

  public int insertOrderItem(
      long orderId, long productId, long companyId, String name, int quantity, BigDecimal price) {
    return db.update(
        "INSERT INTO order_item(order_id,product_id,company_id,name,quantity,unit_price)"
            + " VALUES(?,?,?,?,?,?)",
        orderId,
        productId,
        companyId,
        name,
        quantity,
        price);
  }

  public int decrementStock(int quantity, long productId) {
    return db.update("UPDATE product SET stock=stock-? WHERE id=?", quantity, productId);
  }

  public int insertPayment(long orderId, String method) {
    return db.update("INSERT INTO payment(order_id,method) VALUES(?,?)", orderId, method);
  }

  public int insertDelivery(long orderId) {
    return db.update("INSERT INTO delivery(order_id) VALUES(?)", orderId);
  }

  public int insertRedemption(long couponId, long accountId, long orderId) {
    return db.update(
        "INSERT INTO coupon_redemption(coupon_id,account_id,order_id) VALUES(?,?,?)",
        couponId,
        accountId,
        orderId);
  }

  public int clearCart(long accountId) {
    return db.update("DELETE FROM cart_item WHERE account_id=?", accountId);
  }
}
