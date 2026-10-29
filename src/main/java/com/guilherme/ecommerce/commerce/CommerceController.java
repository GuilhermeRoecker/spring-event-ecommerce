package com.guilherme.ecommerce.commerce;

import com.guilherme.ecommerce.commerce.CommerceDtos.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CommerceController {
  private final CommerceService service;

  public CommerceController(CommerceService service) {
    this.service = service;
  }

  @GetMapping("/me/addresses")
  public Object addresses() {
    return service.addresses();
  }

  @PostMapping("/me/addresses")
  @ResponseStatus(HttpStatus.CREATED)
  public void address(@Valid @RequestBody AddressInput r) {
    service.address(r);
  }

  @PutMapping("/me/addresses/{id}")
  public void editAddress(@PathVariable long id, @Valid @RequestBody AddressInput r) {
    service.editAddress(id, r);
  }

  @DeleteMapping("/me/addresses/{id}")
  public void deleteAddress(@PathVariable long id) {
    service.deleteAddress(id);
  }

  @GetMapping("/me/cart")
  public Object cart() {
    return service.cart();
  }

  @PutMapping("/me/cart/{productId}")
  public void item(@PathVariable long productId, @Valid @RequestBody Quantity r) {
    service.item(productId, r);
  }

  @DeleteMapping("/me/cart/{productId}")
  public void remove(@PathVariable long productId) {
    service.remove(productId);
  }

  @PostMapping("/me/checkout")
  @ResponseStatus(HttpStatus.CREATED)
  public Object checkout(
      @Valid @RequestBody Checkout r,
      @RequestHeader("Idempotency-Key") @Pattern(regexp = "[a-zA-Z0-9_-]{8,80}") String key) {
    return service.checkout(r, key);
  }

  @GetMapping("/me/orders")
  public Object orders(@RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page) {
    return service.orders(page);
  }

  @GetMapping("/me/orders/{id}")
  public Object order(@PathVariable long id) {
    return service.order(id);
  }
}
