import { Component, inject, signal } from "@angular/core";
import { FormControl } from "@angular/forms";
import { firstValueFrom, forkJoin } from "rxjs";
import { UI } from "../../shared/ui";
import {
  ManagedProduct,
  Company,
  Category,
} from "../../shared/models/api.models";
import { CatalogService } from "../catalog/catalog.service";
import { AuthService } from "../../core/auth/auth.service";
import { FeedbackService } from "../../core/services/feedback.service";
import { EditorService } from "./editor.service";
@Component({
  selector: "app-managed-products",
  imports: [...UI],
  templateUrl: "./products.component.html",
})
export class ProductsComponent {
  private readonly catalog = inject(CatalogService);
  private readonly auth = inject(AuthService);
  private readonly feedback = inject(FeedbackService);
  private readonly editor = inject(EditorService);
  private version = 0;
  readonly companies = signal<Company[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly products = signal<ManagedProduct[]>([]);
  readonly company = new FormControl<number | null>(null);
  constructor() {
    void this.feedback.run(async () => {
      const data = await firstValueFrom(
        forkJoin({
          companies: this.catalog.companies(),
          categories: this.catalog.categories(),
        }),
      );
      const companies =
        this.auth.user()?.role === "ADMIN"
          ? data.companies
          : data.companies.filter((c) => c.id === this.auth.user()?.companyId);
      this.companies.set(companies);
      this.categories.set(data.categories);
      this.company.setValue(companies[0]?.id ?? null);
      this.load();
    });
  }
  load() {
    const id = this.company.value;
    const version = ++this.version;
    if (!id) {
      this.products.set([]);
      return;
    }
    void this.feedback.run(async () => {
      const products = await firstValueFrom(this.catalog.managed(id));
      if (version === this.version) this.products.set(products);
    });
  }
  categoryName(id: number) {
    return this.categories().find((c) => c.id === id)?.name ?? "—";
  }
  edit(product?: ManagedProduct) {
    this.editor.open(
      {
        title: product ? "Editar produto" : "Novo produto",
        fields: [
          {
            name: "name",
            label: "Nome do produto",
            value: product?.name,
            maxLength: 160,
          },
          {
            name: "companyId",
            label: "Loja",
            type: "select",
            value: product?.company_id ?? this.company.value,
            options: this.companies(),
            disabled: !!product || this.auth.user()?.role !== "ADMIN",
          },
          {
            name: "categoryId",
            label: "Categoria",
            type: "select",
            value: product?.category_id,
            options: this.categories(),
          },
          {
            name: "description",
            label: "Descrição",
            type: "textarea",
            value: product?.description ?? "",
            maxLength: 2000,
            required: false,
          },
          {
            name: "price",
            label: "Preço (R$)",
            type: "number",
            value: product?.price,
            min: 0.01,
            max: 999999999999.99,
            step: 0.01,
          },
          {
            name: "stock",
            label: "Estoque",
            type: "number",
            value: product?.stock ?? 0,
            min: 0,
            max: 1000000,
            step: 1,
            pattern: "^[0-9]+$",
          },
          {
            name: "imageUrl",
            label: "Link da imagem (HTTPS)",
            type: "url",
            value: product?.image_url ?? "",
            maxLength: 1000,
            required: false,
            pattern: "https://[^\\s]+|^$",
          },
          {
            name: "active",
            label: "Disponível no catálogo",
            type: "checkbox",
            value: product?.active ?? true,
          },
        ],
        save: async (v) => {
          await firstValueFrom(
            this.catalog.saveProduct(
              {
                companyId: Number(v["companyId"]),
                categoryId: Number(v["categoryId"]),
                name: String(v["name"]),
                description: String(v["description"] ?? ""),
                price: Number(v["price"]),
                stock: Number(v["stock"]),
                imageUrl: v["imageUrl"] ? String(v["imageUrl"]) : null,
                active: Boolean(v["active"]),
              },
              product?.id,
            ),
          );
        },
      },
      () => this.load(),
    );
  }
  deactivate(product: ManagedProduct) {
    this.editor.confirm(
      "Desativar produto?",
      product.name +
        " deixará de aparecer no catálogo. O histórico será preservado.",
      async () => {
        await firstValueFrom(this.catalog.deactivate(product.id));
      },
      () => this.load(),
    );
  }
}
