package com.guilherme.ecommerce.account.service;

import com.guilherme.ecommerce.account.domain.Account;
import com.guilherme.ecommerce.account.domain.Role;
import com.guilherme.ecommerce.account.repository.AccountRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {
  private final AccountRepository repository;
  private final Environment env;
  private final PasswordEncoder encoder;

  public AdminBootstrap(AccountRepository repository, Environment env, PasswordEncoder encoder) {
    this.repository = repository;
    this.env = env;
    this.encoder = encoder;
  }

  public void run(ApplicationArguments args) {
    String email = env.getProperty("BOOTSTRAP_ADMIN_EMAIL"),
        password = env.getProperty("BOOTSTRAP_ADMIN_PASSWORD");
    if (email == null && password == null) return;
    if (email == null
        || !email.matches("[^ @]+@[^ @]+\\.[^ @]+")
        || password == null
        || password.length() < 12
        || password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new IllegalStateException(
          "Defina BOOTSTRAP_ADMIN_EMAIL e BOOTSTRAP_ADMIN_PASSWORD (12 a 72 caracteres)");
    email = email.trim().toLowerCase(Locale.ROOT);
    if (!repository.existsByRole(Role.ADMIN))
      repository.save(new Account("Administrador", email, encoder.encode(password), Role.ADMIN));
  }
}
