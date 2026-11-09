package com.guilherme.ecommerce.checkout.controller;

import com.guilherme.ecommerce.checkout.dto.request.Checkout;
import com.guilherme.ecommerce.checkout.service.CheckoutService;
import com.guilherme.ecommerce.order.dto.response.CheckoutResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CheckoutController {
  private final CheckoutService checkout;

  public CheckoutController(CheckoutService checkout) {
    this.checkout = checkout;
  }

  @PostMapping("/me/checkout")
  @ResponseStatus(HttpStatus.CREATED)
  public CheckoutResponse checkout(
      @Valid @RequestBody Checkout r,
      @RequestHeader("Idempotency-Key") @Pattern(regexp = "[a-zA-Z0-9_-]{8,80}") String key) {
    return checkout.checkout(r, key);
  }
}
