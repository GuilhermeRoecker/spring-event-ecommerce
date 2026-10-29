package com.guilherme.ecommerce.auth;

import com.guilherme.ecommerce.shared.Database;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepository {
  private final Database db;

  public AccountRepository(Database db) {
    this.db = db;
  }

  public int insertCustomer(String name, String email, String passwordHash) {
    return db.update(
        "INSERT INTO account(name,email,password_hash,role) VALUES(?,?,?,'CUSTOMER')",
        name,
        email,
        passwordHash);
  }

  public int updateName(String name, long id) {
    return db.update("UPDATE account SET name=? WHERE id=?", name, id);
  }

  public Map<String, Object> lockPassword(long id) {
    return db.one("SELECT password_hash FROM account WHERE id=? FOR UPDATE", id);
  }

  public int updatePassword(String passwordHash, long id) {
    return db.update("UPDATE account SET password_hash=? WHERE id=?", passwordHash, id);
  }

  public List<Map<String, Object>> findAccounts(int offset) {
    return db.list(
        "SELECT id,name,email,role,company_id FROM account ORDER BY id LIMIT 50 OFFSET ?", offset);
  }

  public Map<String, Object> lockAccount(long id) {
    return db.one("SELECT id FROM account WHERE id=? FOR UPDATE", id);
  }

  public Map<String, Object> requireCompany(long id) {
    return db.one("SELECT id FROM company WHERE id=?", id);
  }

  public int updateMembership(String role, Long companyId, long id) {
    return db.update("UPDATE account SET role=?,company_id=? WHERE id=?", role, companyId, id);
  }

  public Map<String, Object> findCurrentByEmail(String email) {
    return db.one("SELECT id,name,email,role,company_id FROM account WHERE email=?", email);
  }

  public List<Map<String, Object>> findCredentialsByEmail(String email) {
    return db.list("SELECT email,password_hash,role FROM account WHERE email=?", email);
  }

  public List<Map<String, Object>> findAdministrators() {
    return db.list("SELECT id FROM account WHERE role='ADMIN'");
  }

  public int insertAdministrator(String email, String passwordHash) {
    return db.update(
        "INSERT INTO account(name,email,password_hash,role) VALUES('Administrador',?,?,'ADMIN')",
        email,
        passwordHash);
  }
}
