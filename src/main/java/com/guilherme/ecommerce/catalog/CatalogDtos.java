package com.guilherme.ecommerce.catalog;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public final class CatalogDtos {
  private CatalogDtos() {}

  public record ProductInput(
      @NotNull @Positive Long companyId,
      @NotNull @Positive Long categoryId,
      @NotBlank @Size(max = 160) String name,
      @NotNull @Size(max = 2000) String description,
      @NotNull
          @DecimalMin("0.01")
          @DecimalMax("999999999999.99")
          @Digits(integer = 12, fraction = 2)
          BigDecimal price,
      @Min(0) @Max(1000000) int stock,
      boolean active,
      @Size(max = 1000) @Pattern(regexp = "https://[^\\s]+|^$", message = "Use HTTPS")
          String imageUrl) {}

  public record CategoryInput(@NotBlank @Size(max = 120) String name) {}

  public record CompanyInput(
      @NotBlank @Size(max = 160) String name, @NotBlank @Size(max = 200) String legalName) {}
}
