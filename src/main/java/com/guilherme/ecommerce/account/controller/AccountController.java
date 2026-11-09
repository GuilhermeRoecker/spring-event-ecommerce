package com.guilherme.ecommerce.account.controller;

import com.guilherme.ecommerce.account.dto.request.Member;
import com.guilherme.ecommerce.account.dto.request.PasswordChange;
import com.guilherme.ecommerce.account.dto.request.Profile;
import com.guilherme.ecommerce.account.dto.request.Registration;
import com.guilherme.ecommerce.account.dto.response.AccountResponse;
import com.guilherme.ecommerce.account.dto.response.CsrfResponse;
import com.guilherme.ecommerce.account.dto.response.MessageResponse;
import com.guilherme.ecommerce.account.dto.response.ProfileResponse;
import com.guilherme.ecommerce.account.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AccountController {
  private final AccountService service;

  public AccountController(AccountService service) {
    this.service = service;
  }

  @GetMapping("/auth/csrf")
  public CsrfResponse csrf(CsrfToken token) {
    return new CsrfResponse(token.getToken(), token.getHeaderName());
  }

  @PostMapping("/auth/register")
  @ResponseStatus(HttpStatus.CREATED)
  public MessageResponse register(@Valid @RequestBody Registration r) {
    return service.register(r);
  }

  @GetMapping("/me")
  public ProfileResponse me() {
    return service.me();
  }

  @PutMapping("/me")
  public ProfileResponse profile(@Valid @RequestBody Profile r) {
    return service.profile(r);
  }

  @PutMapping("/me/password")
  public void password(@Valid @RequestBody PasswordChange r, HttpServletRequest request) {
    service.password(r);
    var session = request.getSession(false);
    if (session != null) session.invalidate();
    SecurityContextHolder.clearContext();
  }

  @GetMapping("/admin/accounts")
  public List<AccountResponse> accounts(
      @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page) {
    return service.accounts(page);
  }

  @PutMapping("/admin/accounts/{id}/membership")
  public void membership(@PathVariable long id, @Valid @RequestBody Member r) {
    service.membership(id, r);
  }
}
