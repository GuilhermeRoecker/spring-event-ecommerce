import { TestBed } from "@angular/core/testing";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { provideHttpClient } from "@angular/common/http";
import { firstValueFrom } from "rxjs";
import { API_URL } from "../../core/services/api.service";
import { CheckoutService } from "./checkout.service";
describe("CheckoutService", () => {
  it("preserves the caller idempotency key across an uncertain request and retry", async () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_URL, useValue: "/api" },
      ],
    });
    const service = TestBed.inject(CheckoutService),
      backend = TestBed.inject(HttpTestingController);
    const input = { addressId: 1, method: "PIX" as const, coupon: null };
    const initial = firstValueFrom(service.submit(input, "checkout-key")).catch(
      (error) => error,
    );
    const request = backend.expectOne("/api/me/checkout");
    expect(request.request.headers.get("Idempotency-Key")).toBe("checkout-key");
    request.error(new ProgressEvent("error"));
    await initial;
    const retry = firstValueFrom(service.submit(input, "checkout-key"));
    const second = backend.expectOne("/api/me/checkout");
    expect(second.request.headers.get("Idempotency-Key")).toBe("checkout-key");
    expect(second.request.body).toEqual(input);
    second.flush({ id: 7, total: 10, status: "AWAITING_PAYMENT" });
    expect((await retry).id).toBe(7);
    backend.verify();
  });
});
