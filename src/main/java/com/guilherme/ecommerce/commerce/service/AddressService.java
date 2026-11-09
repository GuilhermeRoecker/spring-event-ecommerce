package com.guilherme.ecommerce.commerce.service;

import static com.guilherme.ecommerce.shared.Resources.require;

import com.guilherme.ecommerce.account.security.CurrentAccount;
import com.guilherme.ecommerce.account.service.AccountLockService;
import com.guilherme.ecommerce.commerce.domain.Address;
import com.guilherme.ecommerce.commerce.dto.request.AddressInput;
import com.guilherme.ecommerce.commerce.dto.response.AddressResponse;
import com.guilherme.ecommerce.commerce.repository.AddressRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class AddressService {
  private final AddressRepository addresses;
  private final CurrentAccount current;
  private final AccountLockService locks;

  public AddressService(
      AddressRepository addresses, CurrentAccount current, AccountLockService locks) {
    this.addresses = addresses;
    this.current = current;
    this.locks = locks;
  }

  public List<AddressResponse> addresses() {
    return addresses
        .findByAccountIdOrderByIdAsc(current.get().id(), PageRequest.of(0, 100))
        .stream()
        .map(
            a ->
                new AddressResponse(
                    a.getId(),
                    a.getStreet(),
                    a.getNumber(),
                    a.getComplement(),
                    a.getCity(),
                    a.getState(),
                    a.getPostalCode()))
        .toList();
  }

  @Transactional
  public void address(@Valid AddressInput r) {
    var a = locks.lock(current.get().id());
    addresses.save(
        new Address(
            a, r.street(), r.number(), r.complement(), r.city(), r.state(), r.postalCode()));
  }

  @Transactional
  public void editAddress(long id, @Valid AddressInput r) {
    long u = current.get().id();
    locks.lock(u);
    require(addresses.findByIdAndAccountId(id, u))
        .revise(r.street(), r.number(), r.complement(), r.city(), r.state(), r.postalCode());
  }

  @Transactional
  public void deleteAddress(long id) {
    long u = current.get().id();
    locks.lock(u);
    addresses.delete(require(addresses.findByIdAndAccountId(id, u)));
  }
}
