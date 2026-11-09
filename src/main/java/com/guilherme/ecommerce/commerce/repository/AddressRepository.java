package com.guilherme.ecommerce.commerce.repository;

import com.guilherme.ecommerce.commerce.domain.Address;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Long> {
  List<Address> findByAccountIdOrderByIdAsc(long accountId, Pageable page);

  Optional<Address> findByIdAndAccountId(long id, long accountId);
}
