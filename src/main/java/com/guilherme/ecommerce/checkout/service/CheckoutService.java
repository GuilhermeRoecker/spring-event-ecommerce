package com.guilherme.ecommerce.checkout.service;

import static com.guilherme.ecommerce.shared.Resources.require;

import com.guilherme.ecommerce.account.security.CurrentAccount;
import com.guilherme.ecommerce.account.service.AccountLockService;
import com.guilherme.ecommerce.catalog.domain.Product;
import com.guilherme.ecommerce.catalog.repository.ProductRepository;
import com.guilherme.ecommerce.checkout.domain.CouponRedemption;
import com.guilherme.ecommerce.checkout.dto.request.Checkout;
import com.guilherme.ecommerce.checkout.repository.RedemptionRepository;
import com.guilherme.ecommerce.commerce.repository.AddressRepository;
import com.guilherme.ecommerce.commerce.repository.CartRepository;
import com.guilherme.ecommerce.coupon.domain.Coupon;
import com.guilherme.ecommerce.coupon.repository.CouponRepository;
import com.guilherme.ecommerce.order.domain.Delivery;
import com.guilherme.ecommerce.order.domain.OrderItem;
import com.guilherme.ecommerce.order.domain.Payment;
import com.guilherme.ecommerce.order.domain.PurchaseOrder;
import com.guilherme.ecommerce.order.dto.response.CheckoutResponse;
import com.guilherme.ecommerce.order.repository.DeliveryRepository;
import com.guilherme.ecommerce.order.repository.OrderItemRepository;
import com.guilherme.ecommerce.order.repository.OrderRepository;
import com.guilherme.ecommerce.order.repository.PaymentRepository;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
@Transactional(readOnly = true)
public class CheckoutService {
  private final AccountLockService locks;
  private final CurrentAccount current;
  private final CartRepository cart;
  private final AddressRepository addresses;
  private final ProductRepository products;
  private final CouponRepository coupons;
  private final RedemptionRepository redemptions;
  private final OrderRepository orders;
  private final OrderItemRepository items;
  private final PaymentRepository payments;
  private final DeliveryRepository deliveries;

  public CheckoutService(
      AccountLockService locks,
      CurrentAccount current,
      CartRepository cart,
      AddressRepository addresses,
      ProductRepository products,
      CouponRepository coupons,
      RedemptionRepository redemptions,
      OrderRepository orders,
      OrderItemRepository items,
      PaymentRepository payments,
      DeliveryRepository deliveries) {
    this.locks = locks;
    this.current = current;
    this.cart = cart;
    this.addresses = addresses;
    this.products = products;
    this.coupons = coupons;
    this.redemptions = redemptions;
    this.orders = orders;
    this.items = items;
    this.payments = payments;
    this.deliveries = deliveries;
  }

  @Transactional
  public CheckoutResponse checkout(@Valid Checkout r, String key) {
    if (key == null || !key.matches("[a-zA-Z0-9_-]{8,80}"))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chave de idempotência inválida");
    var account = locks.lock(current.get().id());
    long user = account.getId();
    var previous = orders.findByAccountIdAndRequestKey(user, key);
    if (previous.isPresent()) return response(previous.get());
    var address = require(addresses.findByIdAndAccountId(r.addressId(), user));
    // Scalar projection prevents loading Product into the persistence context before locking it.
    var lines = cart.checkoutLines(user);
    if (lines.isEmpty()) throw bad("Carrinho vazio");
    var locked = new ArrayList<Product>();
    BigDecimal subtotal = BigDecimal.ZERO;
    for (var line : lines) {
      var p = require(products.lockById(line.productId()));
      p.purchase(line.quantity());
      subtotal = subtotal.add(p.getPrice().multiply(BigDecimal.valueOf(line.quantity())));
      locked.add(p);
    }
    Coupon coupon = null;
    BigDecimal discount = BigDecimal.ZERO;
    if (r.coupon() != null && !r.coupon().isBlank()) {
      coupon = require(coupons.lockByCode(r.coupon()));
      if (redemptions.existsByCouponIdAndAccountId(coupon.getId(), user))
        throw bad("Cupom já utilizado por esta conta");
      coupon.redeem(LocalDate.now());
      discount = coupon.discount(subtotal);
    }
    var order =
        orders.save(
            new PurchaseOrder(account, key, subtotal, discount, address.snapshot(), coupon));
    for (int i = 0; i < lines.size(); i++)
      items.save(new OrderItem(order, locked.get(i), lines.get(i).quantity()));
    payments.save(new Payment(order, r.method()));
    deliveries.save(new Delivery(order));
    if (coupon != null) redemptions.save(new CouponRedemption(coupon, account, order));
    cart.deleteByAccountId(user);
    return response(order);
  }

  private CheckoutResponse response(PurchaseOrder o) {
    return new CheckoutResponse(o.getId(), o.getTotal(), o.getStatus().name());
  }

  private ResponseStatusException bad(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}
