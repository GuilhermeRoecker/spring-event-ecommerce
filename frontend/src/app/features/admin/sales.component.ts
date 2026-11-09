import { Component, inject, signal } from "@angular/core";
import { FormControl } from "@angular/forms";
import { firstValueFrom } from "rxjs";
import { UI } from "../../shared/ui";
import { Company, Sale } from "../../shared/models/api.models";
import { PagerComponent } from "../../shared/components/pager.component";
import { StatusComponent } from "../../shared/components/status.component";
import { CatalogService } from "../catalog/catalog.service";
import { AuthService } from "../../core/auth/auth.service";
import { FeedbackService } from "../../core/services/feedback.service";
import { AdminService } from "./admin.service";
@Component({
  selector: "app-sales",
  imports: [...UI, PagerComponent, StatusComponent],
  templateUrl: "./sales.component.html",
})
export class SalesComponent {
  private readonly catalog = inject(CatalogService);
  private readonly auth = inject(AuthService);
  private readonly admin = inject(AdminService);
  private readonly feedback = inject(FeedbackService);
  private version = 0;
  readonly companies = signal<Company[]>([]);
  readonly sales = signal<Sale[]>([]);
  readonly company = new FormControl<number | null>(null);
  readonly page = signal(0);
  constructor() {
    void this.feedback.run(async () => {
      const companies = await firstValueFrom(this.catalog.companies());
      const available =
        this.auth.user()?.role === "ADMIN"
          ? companies
          : companies.filter((c) => c.id === this.auth.user()?.companyId);
      this.companies.set(available);
      this.company.setValue(available[0]?.id ?? null);
      this.load(0);
    });
  }
  load(page: number) {
    const company = this.company.value;
    const version = ++this.version;
    if (!company) {
      this.sales.set([]);
      return;
    }
    void this.feedback.run(async () => {
      const sales = await firstValueFrom(this.admin.sales(company, page));
      if (version === this.version) {
        this.sales.set(sales);
        this.page.set(page);
      }
    });
  }
}
