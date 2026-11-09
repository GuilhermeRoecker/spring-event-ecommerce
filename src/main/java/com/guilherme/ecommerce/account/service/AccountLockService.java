package com.guilherme.ecommerce.account.service;

import static com.guilherme.ecommerce.shared.Resources.require;

import com.guilherme.ecommerce.account.domain.Account;
import com.guilherme.ecommerce.account.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class AccountLockService {
  private final AccountRepository accounts;

  public AccountLockService(AccountRepository accounts) {
    this.accounts = accounts;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public Account lock(long id) {
    return require(accounts.lockById(id));
  }
}
