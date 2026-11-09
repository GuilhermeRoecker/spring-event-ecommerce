package com.guilherme.ecommerce.account.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AccountResponse(
    long id, String name, String email, String role, @JsonProperty("company_id") Long companyId) {}
