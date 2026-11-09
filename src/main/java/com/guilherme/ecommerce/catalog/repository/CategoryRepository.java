package com.guilherme.ecommerce.catalog.repository;

import com.guilherme.ecommerce.catalog.domain.Category;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
  List<Category> findAllByOrderByNameAsc(Pageable page);
}
