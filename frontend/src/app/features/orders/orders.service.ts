import { inject, Injectable } from "@angular/core";
import { ApiService } from "../../core/services/api.service";
import { Order, OrderDetail } from "../../shared/models/api.models";
@Injectable({ providedIn: "root" })
export class OrdersService {
  private readonly api = inject(ApiService);
  list(page: number) {
    return this.api.get<Order[]>("/me/orders", { page });
  }
  detail(id: number) {
    return this.api.get<OrderDetail>("/me/orders/" + id);
  }
}
