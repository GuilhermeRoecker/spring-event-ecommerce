package com.guilherme.ecommerce.account.repository;

import com.guilherme.ecommerce.account.domain.Account;
import com.guilherme.ecommerce.account.domain.Role;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AccountRepository extends JpaRepository<Account, Long> {
  Optional<Account> findByEmail(String email);

  boolean existsByRole(Role role);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from Account a where a.id=:id")
  Optional<Account> lockById(long id);

  List<Account> findAllByOrderByIdAsc(Pageable page);
}
