package com.guilherme.ecommerce.auth;

import com.guilherme.ecommerce.auth.AccountDtos.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AccountController {
  private final AccountService service;

  public AccountController(AccountService service) {
    this.service = service;
  }

  @GetMapping("/auth/csrf")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
  }

  @PostMapping("/auth/register")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Map<String, String> register(@Valid @RequestBody Registration r) {
    return service.register(r);
  }

  @GetMapping("/me")
  public CurrentAccount.Account me() {
    return service.me();
  }

  @PutMapping("/me")
  public CurrentAccount.Account profile(@Valid @RequestBody Profile r) {
    return service.profile(r);
  }

  @PutMapping("/me/password")
  public void password(
      @Valid @RequestBody PasswordChange r, jakarta.servlet.http.HttpServletRequest request) {
    service.password(r);
    var session = request.getSession(false);
    if (session != null) session.invalidate();
    org.springframework.security.core.context.SecurityContextHolder.clearContext();
  }

  @GetMapping("/admin/accounts")
  public Object accounts(@RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page) {
    return service.accounts(page);
  }

  @PutMapping("/admin/accounts/{id}/membership")
  public void membership(@PathVariable long id, @Valid @RequestBody Member r) {
    service.membership(id, r);
  }
}
