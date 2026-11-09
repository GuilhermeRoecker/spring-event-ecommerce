package com.guilherme.ecommerce.coupon.repository;

import com.guilherme.ecommerce.coupon.domain.Coupon;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
  List<Coupon> findAllByOrderByIdAsc(Pageable page);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Coupon c where c.code=:code")
  Optional<Coupon> lockByCode(String code);
}
