package com.guilherme.ecommerce.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyInput(
    @NotBlank @Size(max = 160) String name, @NotBlank @Size(max = 200) String legalName) {}
