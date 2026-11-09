package com.guilherme.ecommerce.shared.exception;

/** Business failures are independent of HTTP; the API advice selects the response status. */
public class BusinessException extends RuntimeException {
  public enum Kind {
    INVALID_OPERATION,
    CONFLICT
  }

  private final Kind kind;

  public BusinessException(Kind kind, String message) {
    super(message);
    this.kind = kind;
  }

  public Kind kind() {
    return kind;
  }

  public static BusinessException conflict(String message) {
    return new BusinessException(Kind.CONFLICT, message);
  }

  public static BusinessException invalid(String message) {
    return new BusinessException(Kind.INVALID_OPERATION, message);
  }
}
