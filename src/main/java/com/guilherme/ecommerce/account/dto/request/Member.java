package com.guilherme.ecommerce.account.dto.request;

import com.guilherme.ecommerce.account.domain.Role;
import jakarta.validation.constraints.NotNull;

public record Member(@NotNull Role role, Long companyId) {}
