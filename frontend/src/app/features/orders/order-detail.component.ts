import { Component, inject, signal } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { firstValueFrom } from "rxjs";
import { UI } from "../../shared/ui";
import { OrderDetail } from "../../shared/models/api.models";
import { StatusComponent } from "../../shared/components/status.component";
import { FeedbackService } from "../../core/services/feedback.service";
import { OrdersService } from "./orders.service";
@Component({
  selector: "app-order-detail",
  imports: [...UI, StatusComponent],
  templateUrl: "./order-detail.component.html",
})
export class OrderDetailComponent {
  readonly detail = signal<OrderDetail | null>(null);
  readonly failed = signal(false);
  readonly methods = { PIX: "Pix", CARD: "Cartão", BOLETO: "Boleto" };
  private readonly service = inject(OrdersService);
  private readonly feedback = inject(FeedbackService);
  constructor() {
    const id = Number(inject(ActivatedRoute).snapshot.paramMap.get("id"));
    void this.feedback.run(async () => {
      try {
        this.detail.set(await firstValueFrom(this.service.detail(id)));
      } catch (error) {
        this.failed.set(true);
        throw error;
      }
    });
  }
}
