package com.guilherme.ecommerce.order.repository;

import com.guilherme.ecommerce.order.domain.OrderItem;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
  List<OrderItem> findByOrderIdOrderByIdAsc(long orderId);

  @EntityGraph(attributePaths = {"order"})
  List<OrderItem> findByCompanyIdOrderByIdDesc(long companyId, Pageable page);
}
