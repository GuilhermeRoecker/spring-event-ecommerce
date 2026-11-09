package com.guilherme.ecommerce.account.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class SecurityProblemHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
  private final ObjectMapper json;

  public SecurityProblemHandler(ObjectMapper json) {
    this.json = json;
  }

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException error)
      throws IOException {
    write(response, HttpStatus.UNAUTHORIZED, "Autenticação necessária");
  }

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException error)
      throws IOException {
    write(response, HttpStatus.FORBIDDEN, "Acesso negado ou token CSRF inválido");
  }

  private void write(HttpServletResponse response, HttpStatus status, String detail)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType("application/problem+json");
    response.setCharacterEncoding("UTF-8");
    json.writeValue(response.getOutputStream(), ProblemDetail.forStatusAndDetail(status, detail));
  }
}
