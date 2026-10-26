package com.guilherme.ecommerce.catalog;

import com.guilherme.ecommerce.auth.CurrentAccount;
import com.guilherme.ecommerce.shared.Database;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CatalogController {
  private final Database db;
  private final CurrentAccount current;

  public CatalogController(Database db, CurrentAccount current) {
    this.db = db;
    this.current = current;
  }

  public record ProductInput(
      @NotNull @Positive Long companyId,
      @NotNull @Positive Long categoryId,
      @NotBlank @Size(max = 160) String name,
      @NotNull @Size(max = 2000) String description,
      @NotNull
          @DecimalMin("0.01")
          @DecimalMax("999999999999.99")
          @Digits(integer = 12, fraction = 2)
          BigDecimal price,
      @Min(0) @Max(1000000) int stock,
      boolean active,
      @Size(max = 1000) @Pattern(regexp = "https://[^\\s]+|^$", message = "Use HTTPS")
          String imageUrl) {}

  @GetMapping("/products")
  public Object products(
      @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) Long companyId,
      @RequestParam(defaultValue = "") @Size(max = 100) String search) {
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

  @GetMapping("/products/{id}")
  public Object product(@PathVariable long id) {
    return db.one(
        "SELECT id,company_id,category_id,name,description,price,stock,image_url FROM product WHERE"
            + " id=? AND active=TRUE",
        id);
  }

  @GetMapping("/manage/products")
  public Object managed(@RequestParam long companyId) {
    current.manage(companyId, false);
    return db.list("SELECT * FROM product WHERE company_id=? ORDER BY id LIMIT 100", companyId);
  }

  @PostMapping("/products")
  @ResponseStatus(HttpStatus.CREATED)
  public void create(@Valid @RequestBody ProductInput p) {
    current.manage(p.companyId(), false);
    db.one("SELECT id FROM category WHERE id=?", p.categoryId());
    db.update(
        "INSERT INTO product(company_id,category_id,name,description,price,stock,active,image_url)"
            + " VALUES(?,?,?,?,?,?,?,?)",
        p.companyId(),
        p.categoryId(),
        p.name(),
        p.description(),
        p.price(),
        p.stock(),
        p.active(),
        p.imageUrl());
  }

  @PutMapping("/products/{id}")
  @Transactional
  public void update(@PathVariable long id, @Valid @RequestBody ProductInput p) {
    var old = db.one("SELECT company_id FROM product WHERE id=? FOR UPDATE", id);
    current.manage(db.id(old, "company_id"), false);
    if (db.id(old, "company_id") != p.companyId())
      throw new org.springframework.web.server.ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Não é permitido transferir produtos entre empresas");
    db.one("SELECT id FROM category WHERE id=?", p.categoryId());
    db.update(
        "UPDATE product SET category_id=?,name=?,description=?,price=?,stock=?,active=?,image_url=?"
            + " WHERE id=?",
        p.categoryId(),
        p.name(),
        p.description(),
        p.price(),
        p.stock(),
        p.active(),
        p.imageUrl(),
        id);
  }

  @DeleteMapping("/products/{id}")
  @Transactional
  public void delete(@PathVariable long id) {
    var p = db.one("SELECT company_id FROM product WHERE id=? FOR UPDATE", id);
    current.manage(db.id(p, "company_id"), false);
    db.update("UPDATE product SET active=FALSE WHERE id=?", id);
  }

  public record CategoryInput(@NotBlank @Size(max = 120) String name) {}

  @GetMapping("/categories")
  public Object categories() {
    return db.list("SELECT id,name FROM category ORDER BY name LIMIT 500");
  }

  @PostMapping("/categories")
  @ResponseStatus(HttpStatus.CREATED)
  public void category(@Valid @RequestBody CategoryInput r) {
    current.admin();
    db.update("INSERT INTO category(name) VALUES(?)", r.name());
  }

  @PutMapping("/categories/{id}")
  public void categoryUpdate(@PathVariable long id, @Valid @RequestBody CategoryInput r) {
    current.admin();
    db.one("SELECT id FROM category WHERE id=?", id);
    db.update("UPDATE category SET name=? WHERE id=?", r.name(), id);
  }

  @DeleteMapping("/categories/{id}")
  public void categoryDelete(@PathVariable long id) {
    current.admin();
    db.one("SELECT id FROM category WHERE id=?", id);
    db.update("DELETE FROM category WHERE id=?", id);
  }

  public record CompanyInput(
      @NotBlank @Size(max = 160) String name, @NotBlank @Size(max = 200) String legalName) {}

  @GetMapping("/companies")
  public Object companies() {
    return db.list("SELECT id,name,legal_name FROM company ORDER BY id LIMIT 500");
  }

  @PostMapping("/companies")
  @ResponseStatus(HttpStatus.CREATED)
  public void company(@Valid @RequestBody CompanyInput r) {
    current.admin();
    db.update("INSERT INTO company(name,legal_name) VALUES(?,?)", r.name(), r.legalName());
  }

  @PutMapping("/companies/{id}")
  public void companyUpdate(@PathVariable long id, @Valid @RequestBody CompanyInput r) {
    current.manage(id, true);
    db.one("SELECT id FROM company WHERE id=?", id);
    db.update("UPDATE company SET name=?,legal_name=? WHERE id=?", r.name(), r.legalName(), id);
  }
}
