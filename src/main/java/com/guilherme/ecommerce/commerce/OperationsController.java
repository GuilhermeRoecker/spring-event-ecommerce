package com.guilherme.ecommerce.commerce;

import com.guilherme.ecommerce.commerce.OperationsDtos.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class OperationsController {
  private final OperationsService service;

  public OperationsController(OperationsService service) {
    this.service = service;
  }

  @GetMapping("/admin/coupons")
  public Object coupons() {
    return service.coupons();
  }

  @PostMapping("/admin/coupons")
  @ResponseStatus(HttpStatus.CREATED)
  public void coupon(@Valid @RequestBody Coupon r) {
    service.coupon(r);
  }

  @DeleteMapping("/admin/coupons/{id}")
  public void disable(@PathVariable long id) {
    service.disable(id);
  }

  @GetMapping("/manage/order-items")
  public Object sales(
      @RequestParam long companyId,
      @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page) {
    return service.sales(companyId, page);
  }

  @PostMapping("/admin/orders/{id}/confirm-payment")
  public void confirm(@PathVariable long id) {
    service.confirm(id);
  }

  @PutMapping("/admin/orders/{id}/delivery")
  public void tracking(@PathVariable long id, @Valid @RequestBody Tracking r) {
    service.tracking(id, r);
  }
}
