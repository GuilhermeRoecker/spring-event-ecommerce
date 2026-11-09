import { inject, Injectable } from "@angular/core";
import { ApiService } from "../../core/services/api.service";
import {
  Category,
  Company,
  Product,
  ManagedProduct,
  ProductInput,
} from "../../shared/models/api.models";
@Injectable({ providedIn: "root" })
export class CatalogService {
  private readonly api = inject(ApiService);
  list(params: Record<string, string | number>) {
    return this.api.get<Product[]>("/products", params);
  }
  product(id: number) {
    return this.api.get<Product>("/products/" + id);
  }
  companies() {
    return this.api.get<Company[]>("/companies");
  }
  categories() {
    return this.api.get<Category[]>("/categories");
  }
  managed(companyId: number) {
    return this.api.get<ManagedProduct[]>("/manage/products", { companyId });
  }
  saveProduct(input: ProductInput, id?: number) {
    return id
      ? this.api.put<void>("/products/" + id, input)
      : this.api.post<void>("/products", input);
  }
  deactivate(id: number) {
    return this.api.delete("/products/" + id);
  }
}
