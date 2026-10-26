package com.guilherme.ecommerce.shared;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class Errors {
  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<ProblemDetail> status(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode())
        .body(
            ProblemDetail.forStatusAndDetail(
                e.getStatusCode(), e.getReason() == null ? "Requisição recusada" : e.getReason()));
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class,
    ConstraintViolationException.class,
    org.springframework.web.method.annotation.HandlerMethodValidationException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
  })
  ProblemDetail invalid(Exception e) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        "Dados inválidos: confira os campos, tipos e limites da requisição");
  }

  @ExceptionHandler({DataIntegrityViolationException.class, ConcurrencyFailureException.class})
  ProblemDetail conflict(Exception e) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT,
        "Conflito de dados ou operação concorrente; confira os dados e tente novamente");
  }
}
