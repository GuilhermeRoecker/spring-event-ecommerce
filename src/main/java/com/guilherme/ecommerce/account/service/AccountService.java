package com.guilherme.ecommerce.account.service;

import static com.guilherme.ecommerce.shared.Resources.require;

import com.guilherme.ecommerce.account.domain.Account;
import com.guilherme.ecommerce.account.domain.Role;
import com.guilherme.ecommerce.account.dto.request.Member;
import com.guilherme.ecommerce.account.dto.request.PasswordChange;
import com.guilherme.ecommerce.account.dto.request.Profile;
import com.guilherme.ecommerce.account.dto.request.Registration;
import com.guilherme.ecommerce.account.dto.response.AccountResponse;
import com.guilherme.ecommerce.account.dto.response.MessageResponse;
import com.guilherme.ecommerce.account.dto.response.ProfileResponse;
import com.guilherme.ecommerce.account.repository.AccountRepository;
import com.guilherme.ecommerce.account.repository.CompanyRepository;
import com.guilherme.ecommerce.account.security.CurrentAccount;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
@Transactional(readOnly = true)
public class AccountService {
  private final AccountRepository repository;
  private final CompanyRepository companies;
  private final CurrentAccount current;
  private final PasswordEncoder passwords;

  public AccountService(
      AccountRepository repository,
      CompanyRepository companies,
      CurrentAccount current,
      PasswordEncoder passwords) {
    this.repository = repository;
    this.companies = companies;
    this.current = current;
    this.passwords = passwords;
  }

  @Transactional
  public MessageResponse register(@Valid Registration r) {
    checkPassword(r.password());
    repository.save(
        new Account(
            r.name(),
            r.email().trim().toLowerCase(Locale.ROOT),
            passwords.encode(r.password()),
            Role.CUSTOMER));
    return new MessageResponse("Conta criada");
  }

  public ProfileResponse me() {
    return current.get();
  }

  @Transactional
  public ProfileResponse profile(@Valid Profile r) {
    var a = require(repository.findById(current.get().id()));
    a.rename(r.name());
    return profile(a);
  }

  @Transactional
  public void password(@Valid PasswordChange r) {
    checkPassword(r.newPassword());
    var a = require(repository.lockById(current.get().id()));
    if (!passwords.matches(r.currentPassword(), a.getPasswordHash()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha atual inválida");
    a.changePassword(passwords.encode(r.newPassword()));
  }

  public List<AccountResponse> accounts(int page) {
    current.admin();
    return repository.findAllByOrderByIdAsc(PageRequest.of(page, 50)).stream()
        .map(
            a ->
                new AccountResponse(
                    a.getId(),
                    a.getName(),
                    a.getEmail(),
                    a.getRole().name(),
                    a.getCompany() == null ? null : a.getCompany().getId()))
        .toList();
  }

  @Transactional
  public void membership(long id, @Valid Member r) {
    current.admin();
    var a = require(repository.lockById(id));
    if ((r.role() == Role.SELLER || r.role() == Role.OWNER) && r.companyId() == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empresa obrigatória");
    a.changeMembership(
        r.role(), r.companyId() == null ? null : require(companies.findById(r.companyId())));
  }

  private ProfileResponse profile(Account a) {
    return new ProfileResponse(
        a.getId(),
        a.getName(),
        a.getEmail(),
        a.getRole().name(),
        a.getCompany() == null ? null : a.getCompany().getId());
  }

  private void checkPassword(String value) {
    int bytes = value.getBytes(StandardCharsets.UTF_8).length;
    if (value.length() < 12 || bytes > 72)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Senha deve ter ao menos 12 caracteres e no máximo 72 bytes em UTF-8");
  }
}
