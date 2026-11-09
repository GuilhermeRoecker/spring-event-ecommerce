import { inject, Injectable } from "@angular/core";
import { firstValueFrom } from "rxjs";
import { ApiService } from "../../core/services/api.service";
import { CartItem, Product } from "../../shared/models/api.models";
@Injectable({ providedIn: "root" })
export class CartService {
  private readonly api = inject(ApiService);
  list() {
    return this.api.get<CartItem[]>("/me/cart");
  }
  set(productId: number, quantity: number) {
    return this.api.put<void>("/me/cart/" + productId, { quantity });
  }
  remove(productId: number) {
    return this.api.delete("/me/cart/" + productId);
  }
  async add(product: Product) {
    const cart = await firstValueFrom(this.list());
    await firstValueFrom(
      this.set(
        product.id,
        (cart.find((item) => item.product_id === product.id)?.quantity ?? 0) +
          1,
      ),
    );
  }
}
