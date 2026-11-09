package com.guilherme.ecommerce.commerce.repository;

import com.guilherme.ecommerce.commerce.domain.CartItem;
import com.guilherme.ecommerce.commerce.domain.CartItemId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CartRepository extends JpaRepository<CartItem, CartItemId> {
  @EntityGraph(attributePaths = {"product"})
  List<CartItem> findByAccountIdOrderByProductIdAsc(long accountId);

  Optional<CartItem> findByAccountIdAndProductId(long accountId, long productId);

  void deleteByAccountIdAndProductId(long accountId, long productId);

  @Query(
      "select new com.guilherme.ecommerce.commerce.repository.CartLine(c.product.id,c.quantity)"
          + " from CartItem c where c.account.id=:accountId order by c.product.id")
  List<CartLine> checkoutLines(long accountId);

  void deleteByAccountId(long accountId);
}
