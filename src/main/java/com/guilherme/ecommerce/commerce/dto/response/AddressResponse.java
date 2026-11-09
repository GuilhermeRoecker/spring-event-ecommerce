package com.guilherme.ecommerce.commerce.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AddressResponse(
    long id,
    String street,
    String number,
    String complement,
    String city,
    String state,
    @JsonProperty("postal_code") String postalCode) {}
