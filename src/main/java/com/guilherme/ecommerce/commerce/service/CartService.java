package com.guilherme.ecommerce.commerce.service;

import static com.guilherme.ecommerce.shared.Resources.require;

import com.guilherme.ecommerce.account.security.CurrentAccount;
import com.guilherme.ecommerce.account.service.AccountLockService;
import com.guilherme.ecommerce.catalog.repository.ProductRepository;
import com.guilherme.ecommerce.commerce.domain.CartItem;
import com.guilherme.ecommerce.commerce.dto.request.Quantity;
import com.guilherme.ecommerce.commerce.dto.response.CartItemResponse;
import com.guilherme.ecommerce.commerce.repository.CartRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
@Transactional(readOnly = true)
public class CartService {
  private final CartRepository cart;
  private final ProductRepository products;
  private final CurrentAccount current;
  private final AccountLockService locks;

  public CartService(
      CartRepository cart,
      ProductRepository products,
      CurrentAccount current,
      AccountLockService locks) {
    this.cart = cart;
    this.products = products;
    this.current = current;
    this.locks = locks;
  }

  public List<CartItemResponse> cart() {
    return cart.findByAccountIdOrderByProductIdAsc(current.get().id()).stream()
        .map(
            i -> {
              var p = i.getProduct();
              return new CartItemResponse(
                  p.getId(),
                  p.getName(),
                  p.getPrice(),
                  p.getStock(),
                  p.getActive(),
                  i.getQuantity());
            })
        .toList();
  }

  @Transactional
  public void item(long productId, @Valid Quantity r) {
    var account = locks.lock(current.get().id());
    var p = require(products.findByIdAndActiveTrue(productId));
    if (r.quantity() < 1 || r.quantity() > 999)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantidade inválida");
    if (p.getStock() < r.quantity())
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Estoque insuficiente");
    var item = cart.findByAccountIdAndProductId(account.getId(), productId);
    if (item.isPresent()) item.get().changeQuantity(r.quantity());
    else cart.save(new CartItem(account, p, r.quantity()));
  }

  @Transactional
  public void remove(long productId) {
    long u = current.get().id();
    locks.lock(u);
    cart.deleteByAccountIdAndProductId(u, productId);
  }
}
