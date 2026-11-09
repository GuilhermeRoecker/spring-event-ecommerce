package com.guilherme.ecommerce.catalog.repository;

import com.guilherme.ecommerce.catalog.domain.Product;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {
  @Query(
      "select p from Product p where p.active=true and (:categoryId is null or"
          + " p.category.id=:categoryId) and (:companyId is null or p.company.id=:companyId) and"
          + " locate(lower(:search),lower(p.name))>0 order by p.id")
  List<Product> catalog(Long categoryId, Long companyId, String search, Pageable page);

  Optional<Product> findByIdAndActiveTrue(long id);

  List<Product> findByCompanyIdOrderByIdAsc(long companyId, Pageable page);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Product p where p.id=:id")
  Optional<Product> lockById(long id);
}
