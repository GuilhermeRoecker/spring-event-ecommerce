import { inject, Injectable } from "@angular/core";
import { ApiService } from "../../core/services/api.service";
import {
  Member,
  Role,
  Coupon,
  CouponInput,
  Sale,
} from "../../shared/models/api.models";
@Injectable({ providedIn: "root" })
export class AdminService {
  private readonly api = inject(ApiService);
  company(name: string, legalName: string, id?: number) {
    return id
      ? this.api.put<void>("/companies/" + id, { name, legalName })
      : this.api.post<void>("/companies", { name, legalName });
  }
  category(name: string, id?: number) {
    return id
      ? this.api.put<void>("/categories/" + id, { name })
      : this.api.post<void>("/categories", { name });
  }
  deleteCategory(id: number) {
    return this.api.delete("/categories/" + id);
  }
  members(page: number) {
    return this.api.get<Member[]>("/admin/accounts", { page });
  }
  membership(id: number, role: Role, companyId: number | null) {
    return this.api.put<void>("/admin/accounts/" + id + "/membership", {
      role,
      companyId,
    });
  }
  coupons() {
    return this.api.get<Coupon[]>("/admin/coupons");
  }
  coupon(input: CouponInput) {
    return this.api.post<void>("/admin/coupons", input);
  }
  disableCoupon(id: number) {
    return this.api.delete("/admin/coupons/" + id);
  }
  sales(companyId: number, page: number) {
    return this.api.get<Sale[]>("/manage/order-items", { companyId, page });
  }
  confirmPayment(id: number) {
    return this.api.post<void>("/admin/orders/" + id + "/confirm-payment");
  }
  track(
    id: number,
    status: "SHIPPED" | "DELIVERED",
    carrier: string,
    trackingCode: string,
  ) {
    return this.api.put<void>("/admin/orders/" + id + "/delivery", {
      status,
      carrier,
      trackingCode,
    });
  }
}
