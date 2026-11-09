package com.guilherme.ecommerce.commerce.controller;

import com.guilherme.ecommerce.commerce.dto.request.AddressInput;
import com.guilherme.ecommerce.commerce.dto.response.AddressResponse;
import com.guilherme.ecommerce.commerce.service.AddressService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AddressController {
  private final AddressService addresses;

  public AddressController(AddressService addresses) {
    this.addresses = addresses;
  }

  @GetMapping("/me/addresses")
  public List<AddressResponse> addresses() {
    return addresses.addresses();
  }

  @PostMapping("/me/addresses")
  @ResponseStatus(HttpStatus.CREATED)
  public void address(@Valid @RequestBody AddressInput r) {
    addresses.address(r);
  }

  @PutMapping("/me/addresses/{id}")
  public void editAddress(@PathVariable long id, @Valid @RequestBody AddressInput r) {
    addresses.editAddress(id, r);
  }

  @DeleteMapping("/me/addresses/{id}")
  public void deleteAddress(@PathVariable long id) {
    addresses.deleteAddress(id);
  }
}
