package com.guilherme.ecommerce.catalog;

import com.guilherme.ecommerce.shared.Database;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogRepository {
  private final Database db;

  public CatalogRepository(Database db) {
    this.db = db;
  }

  public List<Map<String, Object>> findProducts(
      int page, int size, Long categoryId, Long companyId, String search) {
    String sql =
        "SELECT id,company_id,category_id,name,description,price,stock,image_url FROM product WHERE"
            + " active=TRUE";
    List<Object> params = new ArrayList<>();
    if (categoryId != null) {
      sql += " AND category_id=?";
      params.add(categoryId);
    }
    if (companyId != null) {
      sql += " AND company_id=?";
      params.add(companyId);
    }
    sql += " AND POSITION(LOWER(?) IN LOWER(name))>0 ORDER BY id LIMIT ? OFFSET ?";
    params.add(search);
    params.add(size);
    params.add(page * size);
    return db.list(sql, params.toArray());
  }

  public Map<String, Object> findActiveProduct(long id) {
    return db.one(
        "SELECT id,company_id,category_id,name,description,price,stock,image_url FROM product WHERE"
            + " id=? AND active=TRUE",
        id);
  }

  public List<Map<String, Object>> findManagedProducts(long companyId) {
    return db.list("SELECT * FROM product WHERE company_id=? ORDER BY id LIMIT 100", companyId);
  }

  public Map<String, Object> requireCategory(long id) {
    return db.one("SELECT id FROM category WHERE id=?", id);
  }

  public int insertProduct(
      long companyId,
      long categoryId,
      String name,
      String description,
      BigDecimal price,
      int stock,
      boolean active,
      String imageUrl) {
    return db.update(
        "INSERT INTO product(company_id,category_id,name,description,price,stock,active,image_url)"
            + " VALUES(?,?,?,?,?,?,?,?)",
        companyId,
        categoryId,
        name,
        description,
        price,
        stock,
        active,
        imageUrl);
  }

  public Map<String, Object> lockProduct(long id) {
    return db.one("SELECT company_id FROM product WHERE id=? FOR UPDATE", id);
  }

  public Map<String, Object> requireCategoryForUpdate(long id) {
    return db.one("SELECT id FROM category WHERE id=?", id);
  }

  public int updateProduct(
      long categoryId,
      String name,
      String description,
      BigDecimal price,
      int stock,
      boolean active,
      String imageUrl,
      long id) {
    return db.update(
        "UPDATE product SET category_id=?,name=?,description=?,price=?,stock=?,active=?,image_url=?"
            + " WHERE id=?",
        categoryId,
        name,
        description,
        price,
        stock,
        active,
        imageUrl,
        id);
  }

  public Map<String, Object> lockProductForDeletion(long id) {
    return db.one("SELECT company_id FROM product WHERE id=? FOR UPDATE", id);
  }

  public int deactivateProduct(long id) {
    return db.update("UPDATE product SET active=FALSE WHERE id=?", id);
  }

  public List<Map<String, Object>> findCategories() {
    return db.list("SELECT id,name FROM category ORDER BY name LIMIT 500");
  }

  public int insertCategory(String name) {
    return db.update("INSERT INTO category(name) VALUES(?)", name);
  }

  public Map<String, Object> requireCategoryForUpdateById(long id) {
    return db.one("SELECT id FROM category WHERE id=?", id);
  }

  public int updateCategory(String name, long id) {
    return db.update("UPDATE category SET name=? WHERE id=?", name, id);
  }

  public Map<String, Object> requireCategoryForDeletion(long id) {
    return db.one("SELECT id FROM category WHERE id=?", id);
  }

  public int deleteCategory(long id) {
    return db.update("DELETE FROM category WHERE id=?", id);
  }

  public List<Map<String, Object>> findCompanies() {
    return db.list("SELECT id,name,legal_name FROM company ORDER BY id LIMIT 500");
  }

  public int insertCompany(String name, String legalName) {
    return db.update("INSERT INTO company(name,legal_name) VALUES(?,?)", name, legalName);
  }

  public Map<String, Object> requireCompany(long id) {
    return db.one("SELECT id FROM company WHERE id=?", id);
  }

  public int updateCompany(String name, String legalName, long id) {
    return db.update("UPDATE company SET name=?,legal_name=? WHERE id=?", name, legalName, id);
  }
}
