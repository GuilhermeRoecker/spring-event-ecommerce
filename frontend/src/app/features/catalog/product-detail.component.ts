import { Component, inject, signal } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { firstValueFrom } from "rxjs";
import { UI } from "../../shared/ui";
import { Product } from "../../shared/models/api.models";
import { CatalogService } from "./catalog.service";
import { CartService } from "../cart/cart.service";
import { AuthService } from "../../core/auth/auth.service";
import { FeedbackService } from "../../core/services/feedback.service";
@Component({
  selector: "app-product-detail",
  imports: [...UI],
  templateUrl: "./product-detail.component.html",
})
export class ProductDetailComponent {
  readonly product = signal<Product | null>(null);
  readonly failed = signal(false);
  readonly saving = signal(false);
  private readonly catalog = inject(CatalogService);
  private readonly cart = inject(CartService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly feedback = inject(FeedbackService);
  constructor() {
    const id = Number(inject(ActivatedRoute).snapshot.paramMap.get("id"));
    void this.feedback.run(async () => {
      try {
        this.product.set(await firstValueFrom(this.catalog.product(id)));
      } catch (error) {
        this.failed.set(true);
        throw error;
      }
    });
  }
  add(product: Product) {
    if (!this.auth.user()) {
      void this.router.navigate(["/login"], {
        queryParams: { returnUrl: "/products/" + product.id },
      });
      return;
    }
    this.saving.set(true);
    void this.feedback.run(async () => {
      try {
        await this.cart.add(product);
        this.feedback.notify("Produto adicionado ao carrinho.");
      } finally {
        this.saving.set(false);
      }
    });
  }
}
