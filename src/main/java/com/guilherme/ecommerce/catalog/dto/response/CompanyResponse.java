package com.guilherme.ecommerce.catalog.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CompanyResponse(long id, String name, @JsonProperty("legal_name") String legalName) {}
