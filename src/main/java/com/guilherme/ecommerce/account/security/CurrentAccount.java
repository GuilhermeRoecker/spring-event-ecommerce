package com.guilherme.ecommerce.account.security;

import com.guilherme.ecommerce.account.dto.response.ProfileResponse;
import com.guilherme.ecommerce.account.repository.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentAccount {
  private final AccountRepository repository;

  public CurrentAccount(AccountRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public ProfileResponse get() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken)
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    var a =
        repository
            .findByEmail(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    return new ProfileResponse(
        a.getId(),
        a.getName(),
        a.getEmail(),
        a.getRole().name(),
        a.getCompany() == null ? null : a.getCompany().getId());
  }

  public void admin() {
    if (!get().role().equals("ADMIN")) denied();
  }

  public void manage(long company, boolean ownerOnly) {
    var a = get();
    if (a.role().equals("ADMIN")) return;
    if (a.companyId() == null
        || a.companyId() != company
        || !(a.role().equals("OWNER") || (!ownerOnly && a.role().equals("SELLER")))) denied();
  }

  private void denied() {
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
  }
}
