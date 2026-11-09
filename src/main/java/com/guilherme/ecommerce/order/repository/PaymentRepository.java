package com.guilherme.ecommerce.order.repository;

import com.guilherme.ecommerce.order.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {}
