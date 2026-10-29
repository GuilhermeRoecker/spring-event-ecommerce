package com.guilherme.ecommerce.auth;

import jakarta.validation.constraints.*;

public final class AccountDtos {
  private AccountDtos() {}

  public record Registration(
      @NotBlank @Size(max = 160) String name,
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(min = 12, max = 72) String password) {}

  public record Profile(@NotBlank @Size(max = 160) String name) {}

  public record PasswordChange(
      @NotBlank String currentPassword, @NotBlank @Size(min = 12, max = 72) String newPassword) {}

  public record Member(@NotNull Role role, Long companyId) {}

  public enum Role {
    CUSTOMER,
    SELLER,
    OWNER,
    ADMIN
  }
}
