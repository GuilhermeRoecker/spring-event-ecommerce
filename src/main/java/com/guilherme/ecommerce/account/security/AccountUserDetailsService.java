package com.guilherme.ecommerce.account.security;

import com.guilherme.ecommerce.account.repository.AccountRepository;
import java.util.Locale;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountUserDetailsService implements UserDetailsService {
  private final AccountRepository accounts;

  public AccountUserDetailsService(AccountRepository accounts) {
    this.accounts = accounts;
  }

  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String email) {
    var a =
        accounts
            .findByEmail(email.trim().toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
    return User.withUsername(a.getEmail())
        .password(a.getPasswordHash())
        .roles(a.getRole().name())
        .build();
  }
}
