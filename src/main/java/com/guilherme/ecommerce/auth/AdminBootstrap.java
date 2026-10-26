package com.guilherme.ecommerce.auth;

import com.guilherme.ecommerce.shared.Database;
import java.util.Locale;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {
  private final Database db;
  private final Environment env;
  private final PasswordEncoder encoder;

  public AdminBootstrap(Database db, Environment env, PasswordEncoder encoder) {
    this.db = db;
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
        || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw new IllegalStateException(
          "Defina BOOTSTRAP_ADMIN_EMAIL e BOOTSTRAP_ADMIN_PASSWORD (12 a 72 caracteres)");
    email = email.trim().toLowerCase(Locale.ROOT);
    if (db.list("SELECT id FROM account WHERE role='ADMIN'").isEmpty())
      db.update(
          "INSERT INTO account(name,email,password_hash,role) VALUES('Administrador',?,?,'ADMIN')",
          email,
          encoder.encode(password));
  }
}
