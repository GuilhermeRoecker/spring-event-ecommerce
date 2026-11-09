package com.guilherme.ecommerce.checkout.repository;

import com.guilherme.ecommerce.checkout.domain.CouponRedemption;
import com.guilherme.ecommerce.checkout.domain.CouponRedemptionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RedemptionRepository extends JpaRepository<CouponRedemption, CouponRedemptionId> {
  boolean existsByCouponIdAndAccountId(long couponId, long accountId);
}
