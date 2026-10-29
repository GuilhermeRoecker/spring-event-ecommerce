package com.guilherme.ecommerce.auth;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwords() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  UserDetailsService users(AccountRepository repository) {
    return email -> {
      var rows = repository.findCredentialsByEmail(email.trim().toLowerCase(java.util.Locale.ROOT));
      if (rows.isEmpty()) throw new UsernameNotFoundException("Credenciais inválidas");
      var r = rows.getFirst();
      return User.withUsername((String) r.get("email"))
          .password((String) r.get("password_hash"))
          .roles((String) r.get("role"))
          .build();
    };
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(
            a ->
                a.requestMatchers("/", "/index.html", "/app.js", "/style.css", "/error")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/assets/**")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/auth/csrf",
                        "/api/products",
                        "/api/products/*",
                        "/api/categories",
                        "/api/companies")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .formLogin(
            f ->
                f.loginProcessingUrl("/api/auth/login")
                    .usernameParameter("email")
                    .passwordParameter("password")
                    .successHandler(
                        (req, res, auth) -> {
                          res.setStatus(204);
                        })
                    .failureHandler(
                        (req, res, e) -> {
                          res.setStatus(401);
                          res.setContentType("application/json");
                          res.getWriter().write("{\"error\":\"Credenciais inválidas\"}");
                        }))
        .logout(
            l ->
                l.logoutUrl("/api/auth/logout")
                    .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint((req, res, ex) -> res.sendError(401))
                    .accessDeniedHandler((req, res, ex) -> res.sendError(403)))
        .build();
  }
}
