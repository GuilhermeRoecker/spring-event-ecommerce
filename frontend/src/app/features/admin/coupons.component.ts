import { Component, inject, signal } from "@angular/core";
import { firstValueFrom } from "rxjs";
import { UI } from "../../shared/ui";
import { Coupon } from "../../shared/models/api.models";
import { FeedbackService } from "../../core/services/feedback.service";
import { AdminService } from "./admin.service";
import { EditorService } from "./editor.service";
@Component({
  selector: "app-coupons",
  imports: [...UI],
  templateUrl: "./coupons.component.html",
})
export class CouponsComponent {
  private readonly admin = inject(AdminService);
  private readonly editor = inject(EditorService);
  private readonly feedback = inject(FeedbackService);
  readonly coupons = signal<Coupon[]>([]);
  constructor() {
    this.load();
  }
  load() {
    void this.feedback.run(async () =>
      this.coupons.set(await firstValueFrom(this.admin.coupons())),
    );
  }
  create() {
    this.editor.open(
      {
        title: "Novo cupom",
        fields: [
          {
            name: "code",
            label: "Código",
            maxLength: 40,
            pattern: "[A-Z0-9_-]{3,40}",
          },
          {
            name: "percentage",
            label: "Desconto (%)",
            type: "number",
            min: 1,
            max: 100,
            step: 1,
            pattern: "^[0-9]+$",
          },
          { name: "expiresOn", label: "Válido até", type: "date" },
          {
            name: "usageLimit",
            label: "Limite de usos",
            type: "number",
            value: 100,
            min: 1,
            step: 1,
            pattern: "^[0-9]+$",
          },
        ],
        save: async (v) => {
          await firstValueFrom(
            this.admin.coupon({
              code: String(v["code"]),
              percentage: Number(v["percentage"]),
              expiresOn: String(v["expiresOn"]),
              usageLimit: Number(v["usageLimit"]),
            }),
          );
        },
      },
      () => this.load(),
    );
  }
  disable(coupon: Coupon) {
    this.editor.confirm(
      "Desativar cupom?",
      "O cupom " + coupon.code + " deixará de aceitar novos usos.",
      async () => {
        await firstValueFrom(this.admin.disableCoupon(coupon.id));
      },
      () => this.load(),
    );
  }
}
