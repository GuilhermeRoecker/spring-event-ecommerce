package com.guilherme.ecommerce.commerce;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public final class OperationsDtos {
  private OperationsDtos() {}

  public record Coupon(
      @NotBlank @Pattern(regexp = "[A-Z0-9_-]{3,40}") String code,
      @Min(1) @Max(100) int percentage,
      @NotNull @FutureOrPresent LocalDate expiresOn,
      @Min(1) int usageLimit) {}

  public enum DeliveryStatus {
    SHIPPED,
    DELIVERED
  }

  public record Tracking(
      @NotNull DeliveryStatus status,
      @NotBlank @Size(max = 120) String carrier,
      @NotBlank @Size(max = 120) String trackingCode) {}
}
