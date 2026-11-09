package com.guilherme.ecommerce.coupon.service;

import static com.guilherme.ecommerce.shared.Resources.require;

import com.guilherme.ecommerce.account.security.CurrentAccount;
import com.guilherme.ecommerce.coupon.domain.Coupon;
import com.guilherme.ecommerce.coupon.dto.request.CouponInput;
import com.guilherme.ecommerce.coupon.dto.response.CouponResponse;
import com.guilherme.ecommerce.coupon.repository.CouponRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class CouponService {
  private final CurrentAccount current;
  private final CouponRepository coupons;

  public CouponService(CurrentAccount current, CouponRepository coupons) {
    this.current = current;
    this.coupons = coupons;
  }

  public List<CouponResponse> coupons() {
    current.admin();
    return coupons.findAllByOrderByIdAsc(PageRequest.of(0, 500)).stream()
        .map(
            c ->
                new CouponResponse(
                    c.getId(),
                    c.getCode(),
                    c.getPercentage(),
                    c.getExpiresOn(),
                    c.getUsageLimit(),
                    c.getUsed(),
                    c.getActive()))
        .toList();
  }

  @Transactional
  public void coupon(@jakarta.validation.Valid CouponInput r) {
    current.admin();
    coupons.save(new Coupon(r.code(), r.percentage(), r.expiresOn(), r.usageLimit()));
  }

  @Transactional
  public void disable(long id) {
    current.admin();
    require(coupons.findById(id)).deactivate();
  }
}
