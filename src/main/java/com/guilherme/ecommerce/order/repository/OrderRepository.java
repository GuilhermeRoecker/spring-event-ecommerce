package com.guilherme.ecommerce.order.repository;

import com.guilherme.ecommerce.order.domain.PurchaseOrder;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<PurchaseOrder, Long> {
  Optional<PurchaseOrder> findByAccountIdAndRequestKey(long accountId, String requestKey);

  Optional<PurchaseOrder> findByIdAndAccountId(long id, long accountId);

  List<PurchaseOrder> findByAccountIdOrderByIdDesc(long accountId, Pageable page);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from PurchaseOrder o where o.id=:id")
  Optional<PurchaseOrder> lockById(long id);
}
