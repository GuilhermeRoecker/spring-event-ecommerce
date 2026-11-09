import { Component, inject, signal } from "@angular/core";
import { firstValueFrom } from "rxjs";
import { UI } from "../../shared/ui";
import { Company } from "../../shared/models/api.models";
import { CatalogService } from "../catalog/catalog.service";
import { AuthService } from "../../core/auth/auth.service";
import { FeedbackService } from "../../core/services/feedback.service";
import { AdminService } from "./admin.service";
import { EditorService } from "./editor.service";
@Component({
  selector: "app-companies",
  imports: [...UI],
  templateUrl: "./companies.component.html",
})
export class CompaniesComponent {
  readonly auth = inject(AuthService);
  private readonly catalog = inject(CatalogService);
  private readonly admin = inject(AdminService);
  private readonly editor = inject(EditorService);
  private readonly feedback = inject(FeedbackService);
  readonly companies = signal<Company[]>([]);
  constructor() {
    this.load();
  }
  load() {
    void this.feedback.run(async () => {
      const companies = await firstValueFrom(this.catalog.companies());
      this.companies.set(
        this.auth.user()?.role === "ADMIN"
          ? companies
          : companies.filter(
              (company) => company.id === this.auth.user()?.companyId,
            ),
      );
    });
  }
  edit(company?: Company) {
    this.editor.open(
      {
        title: company ? "Editar empresa" : "Nova empresa",
        fields: [
          {
            name: "name",
            label: "Nome fantasia",
            value: company?.name,
            maxLength: 160,
          },
          {
            name: "legalName",
            label: "Razão social",
            value: company?.legal_name,
            maxLength: 200,
          },
        ],
        save: async (value) => {
          await firstValueFrom(
            this.admin.company(
              String(value["name"]),
              String(value["legalName"]),
              company?.id,
            ),
          );
        },
      },
      () => this.load(),
    );
  }
}
