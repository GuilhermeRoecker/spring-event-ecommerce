package com.guilherme.ecommerce.account.repository;

import com.guilherme.ecommerce.account.domain.Company;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
  List<Company> findAllByOrderByIdAsc(Pageable page);
}
