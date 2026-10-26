package com.guilherme.ecommerce.auth;

import com.guilherme.ecommerce.shared.Database;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentAccount {
  private final Database db;

  public CurrentAccount(Database db) {
    this.db = db;
  }

  public record Account(long id, String name, String email, String role, Long companyId) {
    public boolean admin() {
      return role.equals("ADMIN");
    }
  }

  public Account get() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser"))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    var r =
        db.one("SELECT id,name,email,role,company_id FROM account WHERE email=?", auth.getName());
    return new Account(
        db.id(r, "id"),
        (String) r.get("name"),
        (String) r.get("email"),
        (String) r.get("role"),
        r.get("company_id") == null ? null : db.id(r, "company_id"));
  }

  public void admin() {
    if (!get().admin()) denied();
  }

  public void manage(long company, boolean ownerOnly) {
    var a = get();
    if (a.admin()) return;
    if (a.companyId() == null
        || a.companyId() != company
        || !(a.role().equals("OWNER") || (!ownerOnly && a.role().equals("SELLER")))) denied();
  }

  private void denied() {
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
  }
}
