package com.guilherme.ecommerce.auth;

import com.guilherme.ecommerce.shared.Database;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AccountController {
  private final Database db;
  private final CurrentAccount current;
  private final PasswordEncoder passwords;

  public AccountController(Database db, CurrentAccount current, PasswordEncoder passwords) {
    this.db = db;
    this.current = current;
    this.passwords = passwords;
  }

  public record Registration(
      @NotBlank @Size(max = 160) String name,
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(min = 12, max = 72) String password) {}

  public record Profile(@NotBlank @Size(max = 160) String name) {}

  @GetMapping("/auth/csrf")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
  }

  @PostMapping("/auth/register")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Map<String, String> register(@Valid @RequestBody Registration r) {
    checkPassword(r.password());
    db.update(
        "INSERT INTO account(name,email,password_hash,role) VALUES(?,?,?,'CUSTOMER')",
        r.name(),
        r.email().trim().toLowerCase(Locale.ROOT),
        passwords.encode(r.password()));
    return Map.of("message", "Conta criada");
  }

  @GetMapping("/me")
  public CurrentAccount.Account me() {
    return current.get();
  }

  @PutMapping("/me")
  public CurrentAccount.Account profile(@Valid @RequestBody Profile r) {
    db.update("UPDATE account SET name=? WHERE id=?", r.name(), current.get().id());
    return current.get();
  }

  public record PasswordChange(
      @NotBlank String currentPassword, @NotBlank @Size(min = 12, max = 72) String newPassword) {}

  @PutMapping("/me/password")
  @Transactional
  public void password(
      @Valid @RequestBody PasswordChange r, jakarta.servlet.http.HttpServletRequest request) {
    checkPassword(r.newPassword());
    long id = current.get().id();
    var row = db.one("SELECT password_hash FROM account WHERE id=? FOR UPDATE", id);
    if (!passwords.matches(r.currentPassword(), (String) row.get("password_hash")))
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, "Senha atual inválida");
    db.update(
        "UPDATE account SET password_hash=? WHERE id=?", passwords.encode(r.newPassword()), id);
    var session = request.getSession(false);
    if (session != null) session.invalidate();
    org.springframework.security.core.context.SecurityContextHolder.clearContext();
  }

  private void checkPassword(String value) {
    if (value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, "Senha excede 72 bytes em UTF-8");
  }

  public record Member(@NotNull Role role, Long companyId) {}

  public enum Role {
    CUSTOMER,
    SELLER,
    OWNER,
    ADMIN
  }

  @GetMapping("/admin/accounts")
  public Object accounts(@RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page) {
    current.admin();
    return db.list(
        "SELECT id,name,email,role,company_id FROM account ORDER BY id LIMIT 50 OFFSET ?",
        Math.multiplyExact(page, 50));
  }

  @PutMapping("/admin/accounts/{id}/membership")
  @Transactional
  public void membership(@PathVariable long id, @Valid @RequestBody Member r) {
    current.admin();
    db.one("SELECT id FROM account WHERE id=? FOR UPDATE", id);
    if (r.role() == Role.SELLER || r.role() == Role.OWNER) {
      if (r.companyId() == null)
        throw new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.BAD_REQUEST, "Empresa obrigatória");
      db.one("SELECT id FROM company WHERE id=?", r.companyId());
    }
    db.update(
        "UPDATE account SET role=?,company_id=? WHERE id=?", r.role().name(), r.companyId(), id);
  }
}
