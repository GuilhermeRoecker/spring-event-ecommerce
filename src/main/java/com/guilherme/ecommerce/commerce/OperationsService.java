package com.guilherme.ecommerce.commerce;

import com.guilherme.ecommerce.auth.CurrentAccount;
import com.guilherme.ecommerce.commerce.OperationsDtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OperationsService {
  private final OperationsRepository repository;
  private final CurrentAccount current;

  public OperationsService(OperationsRepository repository, CurrentAccount current) {
    this.repository = repository;
    this.current = current;
  }

  public Object coupons() {
    current.admin();
    return repository.findCoupons();
  }

  public void coupon(Coupon r) {
    current.admin();
    repository.insertCoupon(r.code(), r.percentage(), r.expiresOn(), r.usageLimit());
  }

  public void disable(long id) {
    current.admin();
    repository.requireCoupon(id);
    repository.deactivateCoupon(id);
  }

  public Object sales(long companyId, int page) {
    current.manage(companyId, false);
    return repository.findSales(companyId, page * 50);
  }

  @Transactional
  public void confirm(long id) {
    current.admin();
    var o = repository.lockOrderForPayment(id);
    if (!o.get("status").equals("AWAITING_PAYMENT"))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Pedido não aguarda pagamento");
    repository.markPaymentPaid(id);
    repository.markOrderPaid(id);
  }

  @Transactional
  public void tracking(long id, Tracking r) {
    current.admin();
    var o = repository.lockOrderForDelivery(id);
    String previous = (String) o.get("status");
    if (!(previous.equals("PAID") && r.status() == DeliveryStatus.SHIPPED
        || previous.equals("SHIPPED") && r.status() == DeliveryStatus.DELIVERED))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Transição de entrega inválida");
    repository.updateDelivery(r.status().name(), r.carrier(), r.trackingCode(), id);
    repository.updateOrderStatus(r.status().name(), id);
  }
}
