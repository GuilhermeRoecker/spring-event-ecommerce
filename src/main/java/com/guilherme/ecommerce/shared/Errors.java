package com.guilherme.ecommerce.shared;

import com.guilherme.ecommerce.shared.exception.BusinessException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class Errors {
  @ExceptionHandler(BusinessException.class)
  ProblemDetail business(BusinessException e) {
    return ProblemDetail.forStatusAndDetail(
        e.kind() == BusinessException.Kind.CONFLICT ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST,
        e.getMessage());
  }

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
    HandlerMethodValidationException.class,
    MethodArgumentTypeMismatchException.class,
    MissingRequestHeaderException.class,
    MissingServletRequestParameterException.class
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
