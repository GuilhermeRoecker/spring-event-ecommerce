import { Component, inject, signal } from "@angular/core";
import { FormBuilder } from "@angular/forms";
import { Router } from "@angular/router";
import { firstValueFrom, forkJoin } from "rxjs";
import { UI } from "../../shared/ui";
import { Product, Category, Company } from "../../shared/models/api.models";
import { CatalogService } from "./catalog.service";
import { CartService } from "../cart/cart.service";
import { AuthService } from "../../core/auth/auth.service";
import { FeedbackService } from "../../core/services/feedback.service";
import { PagerComponent } from "../../shared/components/pager.component";
@Component({
  selector: "app-catalog",
  imports: [...UI, PagerComponent],
  templateUrl: "./catalog.component.html",
})
export class CatalogComponent {
  private readonly catalog = inject(CatalogService);
  private readonly cart = inject(CartService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly feedback = inject(FeedbackService);
  readonly products = signal<Product[]>([]);
  readonly companies = signal<Company[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly page = signal(0);
  readonly loaded = signal(false);
  readonly adding = signal<number | null>(null);
  readonly filters = inject(FormBuilder).nonNullable.group({
    search: "",
    categoryId: "" as number | string,
    companyId: "" as number | string,
  });
  private version = 0;
  constructor() {
    void this.feedback.run(async () => {
      const lookups = await firstValueFrom(
        forkJoin({
          companies: this.catalog.companies(),
          categories: this.catalog.categories(),
        }),
      );
      this.companies.set(lookups.companies);
      this.categories.set(lookups.categories);
      this.load(0);
    });
  }
  load(page: number) {
    const version = ++this.version;
    void this.feedback.run(async () => {
      const values = this.filters.getRawValue();
      const params: Record<string, string | number> = {
        page,
        size: 20,
        search: values.search,
      };
      if (values.categoryId) params["categoryId"] = values.categoryId;
      if (values.companyId) params["companyId"] = values.companyId;
      const products = await firstValueFrom(this.catalog.list(params));
      if (version === this.version) {
        this.products.set(products);
        this.page.set(page);
        this.loaded.set(true);
      }
    });
  }
  companyName(id: number) {
    return (
      this.companies().find((company) => company.id === id)?.name ?? "Arôme"
    );
  }
  imageFallback(event: Event) {
    const image = event.target as HTMLImageElement;
    image.style.visibility = "hidden";
  }
  add(product: Product) {
    if (!this.auth.user()) {
      void this.router.navigate(["/login"], {
        queryParams: { returnUrl: "/catalog" },
      });
      return;
    }
    this.adding.set(product.id);
    void this.feedback.run(async () => {
      try {
        await this.cart.add(product);
        this.feedback.notify("Produto adicionado ao carrinho.");
      } finally {
        this.adding.set(null);
      }
    });
  }
}
