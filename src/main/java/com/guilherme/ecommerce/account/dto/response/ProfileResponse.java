package com.guilherme.ecommerce.account.dto.response;

public record ProfileResponse(long id, String name, String email, String role, Long companyId) {}
