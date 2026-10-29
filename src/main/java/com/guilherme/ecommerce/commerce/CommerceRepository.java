package com.guilherme.ecommerce.commerce;

import com.guilherme.ecommerce.shared.Database;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class CommerceRepository {
  private final Database db;

  public CommerceRepository(Database db) {
    this.db = db;
  }

  public List<Map<String, Object>> findAddresses(long accountId) {
    return db.list(
        "SELECT id,street,number,complement,city,state,postal_code FROM address WHERE account_id=?"
            + " ORDER BY id LIMIT 100",
        accountId);
  }

  public int insertAddress(
      long accountId,
      String street,
      String number,
      String complement,
      String city,
      String state,
      String postalCode) {
    return db.update(
        "INSERT INTO address(account_id,street,number,complement,city,state,postal_code)"
            + " VALUES(?,?,?,?,?,?,?)",
        accountId,
        street,
        number,
        complement,
        city,
        state,
        postalCode);
  }

  public Map<String, Object> requireOwnedAddress(long id, long accountId) {
    return db.one("SELECT id FROM address WHERE id=? AND account_id=?", id, accountId);
  }

  public int updateAddress(
      String street,
      String number,
      String complement,
      String city,
      String state,
      String postalCode,
      long id,
      long accountId) {
    return db.update(
        "UPDATE address SET street=?,number=?,complement=?,city=?,state=?,postal_code=? WHERE id=?"
            + " AND account_id=?",
        street,
        number,
        complement,
        city,
        state,
        postalCode,
        id,
        accountId);
  }

  public Map<String, Object> requireAddressForDeletion(long id, long accountId) {
    return db.one("SELECT id FROM address WHERE id=? AND account_id=?", id, accountId);
  }

  public int deleteAddress(long id, long accountId) {
    return db.update("DELETE FROM address WHERE id=? AND account_id=?", id, accountId);
  }

  public List<Map<String, Object>> findCart(long accountId) {
    return db.list(
        "SELECT p.id AS product_id,p.name,p.price,p.stock,p.active,c.quantity FROM cart_item c JOIN"
            + " product p ON p.id=c.product_id WHERE c.account_id=? ORDER BY p.id",
        accountId);
  }

  public Map<String, Object> findAvailableStock(long productId) {
    return db.one("SELECT stock FROM product WHERE id=? AND active=TRUE", productId);
  }

  public int deleteCartItem(long accountId, long productId) {
    return db.update(
        "DELETE FROM cart_item WHERE account_id=? AND product_id=?", accountId, productId);
  }

  public int insertCartItem(long accountId, long productId, int quantity) {
    return db.update(
        "INSERT INTO cart_item(account_id,product_id,quantity) VALUES(?,?,?)",
        accountId,
        productId,
        quantity);
  }

  public int removeCartItem(long accountId, long productId) {
    return db.update(
        "DELETE FROM cart_item WHERE account_id=? AND product_id=?", accountId, productId);
  }

  public List<Map<String, Object>> findOrders(long accountId, int offset) {
    return db.list(
        "SELECT id,created_at,status,subtotal,discount,total FROM purchase_order WHERE account_id=?"
            + " ORDER BY id DESC LIMIT 50 OFFSET ?",
        accountId,
        offset);
  }

  public Map<String, Object> findOwnedOrder(long id, long accountId) {
    return db.one(
        "SELECT id,created_at,status,subtotal,discount,total,shipping_address FROM"
            + " purchase_order WHERE id=? AND account_id=?",
        id,
        accountId);
  }

  public List<Map<String, Object>> findOrderItems(long orderId) {
    return db.list(
        "SELECT product_id,name,quantity,unit_price FROM order_item WHERE order_id=? ORDER BY"
            + " id",
        orderId);
  }

  public Map<String, Object> findPayment(long orderId) {
    return db.one("SELECT method,status FROM payment WHERE order_id=?", orderId);
  }

  public Map<String, Object> findDelivery(long orderId) {
    return db.one("SELECT status,carrier,tracking_code FROM delivery WHERE order_id=?", orderId);
  }
}
