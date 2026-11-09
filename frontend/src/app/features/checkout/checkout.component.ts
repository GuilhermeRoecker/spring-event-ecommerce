import { Component, computed, inject, signal } from "@angular/core";
import { FormBuilder, Validators } from "@angular/forms";
import { Router } from "@angular/router";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { firstValueFrom, forkJoin } from "rxjs";
import { UI } from "../../shared/ui";
import {
  Address,
  CartItem,
  PaymentMethod,
} from "../../shared/models/api.models";
import { CartService } from "../cart/cart.service";
import { AccountService } from "../account/account.service";
import { CheckoutService } from "./checkout.service";
import { FeedbackService } from "../../core/services/feedback.service";
@Component({
  selector: "app-checkout",
  imports: [...UI],
  templateUrl: "./checkout.component.html",
})
export class CheckoutComponent {
  private readonly cart = inject(CartService);
  private readonly account = inject(AccountService);
  private readonly checkout = inject(CheckoutService);
  private readonly feedback = inject(FeedbackService);
  private readonly router = inject(Router);
  readonly addresses = signal<Address[]>([]);
  readonly items = signal<CartItem[]>([]);
  readonly busy = signal(false);
  readonly loaded = signal(false);
  readonly total = computed(() =>
    this.items().reduce((sum, item) => sum + item.price * item.quantity, 0),
  );
  readonly form = inject(FormBuilder).group({
    addressId: [null as number | null, Validators.required],
    method: [
      "PIX" as PaymentMethod,
      { nonNullable: true, validators: [Validators.required] },
    ],
    coupon: ["", { nonNullable: true, validators: [Validators.maxLength(40)] }],
  });
  private key: string | null = null;
  private attempted = false;
  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      if (!this.attempted) this.key = null;
    });
    void this.feedback.run(async () => {
      const data = await firstValueFrom(
        forkJoin({
          addresses: this.account.addresses(),
          items: this.cart.list(),
        }),
      );
      this.addresses.set(data.addresses);
      this.items.set(data.items);
      this.loaded.set(true);
      if (data.addresses.length)
        this.form.controls.addressId.setValue(data.addresses[0]!.id);
    });
  }
  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.busy()) return;
    this.busy.set(true);
    this.key ??= crypto.randomUUID();
    this.attempted = true;
    void this.feedback.run(async () => {
      try {
        const value = this.form.getRawValue();
        const order = await firstValueFrom(
          this.checkout.submit(
            {
              addressId: value.addressId!,
              method: value.method!,
              coupon: value.coupon || null,
            },
            this.key!,
          ),
        );
        this.key = null;
        await this.router.navigate(["/orders", order.id]);
        this.feedback.notify(
          "Pedido criado! Acompanhe a confirmação do pagamento.",
        );
      } finally {
        this.busy.set(false);
      }
    });
  }
}
