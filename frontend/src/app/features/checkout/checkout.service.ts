import { inject, Injectable } from "@angular/core";
import { ApiService } from "../../core/services/api.service";
import { CheckoutInput, CheckoutResult } from "../../shared/models/api.models";
@Injectable({ providedIn: "root" })
export class CheckoutService {
  private readonly api = inject(ApiService);
  submit(input: CheckoutInput, key: string) {
    return this.api.post<CheckoutResult>("/me/checkout", input, {
      "Idempotency-Key": key,
    });
  }
}
