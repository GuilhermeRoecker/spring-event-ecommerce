package com.guilherme.ecommerce.coupon.controller;

import com.guilherme.ecommerce.coupon.dto.request.CouponInput;
import com.guilherme.ecommerce.coupon.dto.response.CouponResponse;
import com.guilherme.ecommerce.coupon.service.CouponService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CouponController {
  private final CouponService coupons;

  public CouponController(CouponService coupons) {
    this.coupons = coupons;
  }

  @GetMapping("/admin/coupons")
  public List<CouponResponse> coupons() {
    return coupons.coupons();
  }

  @PostMapping("/admin/coupons")
  @ResponseStatus(HttpStatus.CREATED)
  public void coupon(@Valid @RequestBody CouponInput r) {
    coupons.coupon(r);
  }

  @DeleteMapping("/admin/coupons/{id}")
  public void disable(@PathVariable long id) {
    coupons.disable(id);
  }
}
