package com.guilherme.ecommerce.commerce;

import jakarta.validation.constraints.*;

public final class CommerceDtos {
  private CommerceDtos() {}

  public record AddressInput(
      @NotBlank @Size(max = 200) String street,
      @NotBlank @Size(max = 30) String number,
      @Size(max = 120) String complement,
      @NotBlank @Size(max = 120) String city,
      @NotBlank
          @Pattern(
              regexp =
                  "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO")
          String state,
      @NotBlank @Pattern(regexp = "[0-9]{8}") String postalCode) {}

  public record Quantity(@Min(1) @Max(999) int quantity) {}

  public enum Method {
    PIX,
    CARD,
    BOLETO
  }

  public record Checkout(
      @NotNull @Positive Long addressId, @NotNull Method method, @Size(max = 40) String coupon) {}
}
