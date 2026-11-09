import { Component, computed, inject, signal } from "@angular/core";
import { FormControl, Validators } from "@angular/forms";
import { firstValueFrom } from "rxjs";
import { UI } from "../../shared/ui";
import { CartItem } from "../../shared/models/api.models";
import { CartService } from "./cart.service";
import { FeedbackService } from "../../core/services/feedback.service";
@Component({
  selector: "app-cart",
  imports: [...UI],
  templateUrl: "./cart.component.html",
})
export class CartComponent {
  private readonly cart = inject(CartService);
  private readonly feedback = inject(FeedbackService);
  readonly items = signal<CartItem[]>([]);
  readonly busy = signal(false);
  private readonly quantities = new Map<number, FormControl<number>>();
  readonly total = computed(() =>
    this.items().reduce((sum, item) => sum + item.price * item.quantity, 0),
  );
  constructor() {
    this.load();
  }
  quantity(id: number) {
    return this.quantities.get(id)!;
  }
  private async refresh() {
    const items = await firstValueFrom(this.cart.list());
    this.quantities.clear();
    for (const item of items)
      this.quantities.set(
        item.product_id,
        new FormControl(item.quantity, {
          nonNullable: true,
          validators: [
            Validators.required,
            Validators.min(1),
            Validators.max(999),
            Validators.pattern(/^[0-9]+$/),
          ],
        }),
      );
    this.items.set(items);
  }
  private load() {
    void this.feedback.run(() => this.refresh());
  }
  private execute(work: () => Promise<void>) {
    if (this.busy()) return;
    this.busy.set(true);
    void this.feedback.run(async () => {
      try {
        await work();
        await this.refresh();
      } finally {
        this.busy.set(false);
      }
    });
  }
  update(id: number) {
    const control = this.quantity(id);
    if (control.invalid) {
      control.markAsTouched();
      return;
    }
    this.execute(async () => {
      await firstValueFrom(this.cart.set(id, control.value));
    });
  }
  remove(id: number) {
    this.execute(async () => {
      await firstValueFrom(this.cart.remove(id));
    });
  }
}
