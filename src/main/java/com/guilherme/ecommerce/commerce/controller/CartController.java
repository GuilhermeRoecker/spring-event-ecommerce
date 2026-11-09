package com.guilherme.ecommerce.commerce.controller;

import com.guilherme.ecommerce.commerce.dto.request.Quantity;
import com.guilherme.ecommerce.commerce.dto.response.CartItemResponse;
import com.guilherme.ecommerce.commerce.service.CartService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CartController {
  private final CartService cart;

  public CartController(CartService cart) {
    this.cart = cart;
  }

  @GetMapping("/me/cart")
  public List<CartItemResponse> cart() {
    return cart.cart();
  }

  @PutMapping("/me/cart/{productId}")
  public void item(@PathVariable long productId, @Valid @RequestBody Quantity r) {
    cart.item(productId, r);
  }

  @DeleteMapping("/me/cart/{productId}")
  public void remove(@PathVariable long productId) {
    cart.remove(productId);
  }
}
