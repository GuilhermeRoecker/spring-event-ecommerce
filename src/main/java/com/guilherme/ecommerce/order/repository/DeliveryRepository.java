package com.guilherme.ecommerce.order.repository;

import com.guilherme.ecommerce.order.domain.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {}
