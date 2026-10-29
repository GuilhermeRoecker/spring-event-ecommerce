package com.guilherme.ecommerce.auth;

import com.guilherme.ecommerce.auth.AccountDtos.*;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
  private final AccountRepository repository;
  private final CurrentAccount current;
  private final PasswordEncoder passwords;

  public AccountService(
      AccountRepository repository, CurrentAccount current, PasswordEncoder passwords) {
    this.repository = repository;
    this.current = current;
    this.passwords = passwords;
  }

  public Map<String, String> register(Registration r) {
    checkPassword(r.password());
    repository.insertCustomer(
        r.name(), r.email().trim().toLowerCase(Locale.ROOT), passwords.encode(r.password()));
    return Map.of("message", "Conta criada");
  }

  public CurrentAccount.Account me() {
    return current.get();
  }

  public CurrentAccount.Account profile(Profile r) {
    repository.updateName(r.name(), current.get().id());
    return current.get();
  }

  @Transactional
  public void password(PasswordChange r) {
    checkPassword(r.newPassword());
    long id = current.get().id();
    var row = repository.lockPassword(id);
    if (!passwords.matches(r.currentPassword(), (String) row.get("password_hash")))
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, "Senha atual inválida");
    repository.updatePassword(passwords.encode(r.newPassword()), id);
  }

  public Object accounts(int page) {
    current.admin();
    return repository.findAccounts(Math.multiplyExact(page, 50));
  }

  @Transactional
  public void membership(long id, Member r) {
    current.admin();
    repository.lockAccount(id);
    if (r.role() == Role.SELLER || r.role() == Role.OWNER) {
      if (r.companyId() == null)
        throw new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.BAD_REQUEST, "Empresa obrigatória");
      repository.requireCompany(r.companyId());
    }
    repository.updateMembership(r.role().name(), r.companyId(), id);
  }

  private void checkPassword(String value) {
    if (value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, "Senha excede 72 bytes em UTF-8");
  }
}
