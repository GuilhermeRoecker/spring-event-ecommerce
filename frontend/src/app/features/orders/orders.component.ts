import { Component, inject, signal } from "@angular/core";
import { firstValueFrom } from "rxjs";
import { UI } from "../../shared/ui";
import { Order } from "../../shared/models/api.models";
import { StatusComponent } from "../../shared/components/status.component";
import { PagerComponent } from "../../shared/components/pager.component";
import { FeedbackService } from "../../core/services/feedback.service";
import { OrdersService } from "./orders.service";
@Component({
  selector: "app-orders",
  imports: [...UI, StatusComponent, PagerComponent],
  templateUrl: "./orders.component.html",
})
export class OrdersComponent {
  private readonly service = inject(OrdersService);
  private readonly feedback = inject(FeedbackService);
  readonly orders = signal<Order[]>([]);
  readonly page = signal(0);
  private version = 0;
  constructor() {
    this.load(0);
  }
  load(page: number) {
    const version = ++this.version;
    void this.feedback.run(async () => {
      const orders = await firstValueFrom(this.service.list(page));
      if (version === this.version) {
        this.orders.set(orders);
        this.page.set(page);
      }
    });
  }
}
