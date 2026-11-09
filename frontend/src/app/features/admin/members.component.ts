import { Component, inject, signal } from "@angular/core";
import { firstValueFrom } from "rxjs";
import { UI } from "../../shared/ui";
import {
  Member,
  Company,
  Role,
  ROLE_NAMES,
} from "../../shared/models/api.models";
import { CatalogService } from "../catalog/catalog.service";
import { PagerComponent } from "../../shared/components/pager.component";
import { FeedbackService } from "../../core/services/feedback.service";
import { AdminService } from "./admin.service";
import { EditorService } from "./editor.service";
@Component({
  selector: "app-members",
  imports: [...UI, PagerComponent],
  templateUrl: "./members.component.html",
})
export class MembersComponent {
  private readonly admin = inject(AdminService);
  private readonly catalog = inject(CatalogService);
  private readonly editor = inject(EditorService);
  private readonly feedback = inject(FeedbackService);
  readonly members = signal<Member[]>([]);
  readonly companies = signal<Company[]>([]);
  readonly page = signal(0);
  readonly roles = ROLE_NAMES;
  constructor() {
    this.load(0);
    void this.feedback.run(async () =>
      this.companies.set(await firstValueFrom(this.catalog.companies())),
    );
  }
  load(page: number) {
    void this.feedback.run(async () => {
      this.members.set(await firstValueFrom(this.admin.members(page)));
      this.page.set(page);
    });
  }
  companyName(id: number | null) {
    return this.companies().find((c) => c.id === id)?.name ?? "—";
  }
  edit(member: Member) {
    this.editor.open(
      {
        title: "Acesso de " + member.name,
        description:
          "Para vendedor e dono de loja, selecione a empresa correspondente.",
        fields: [
          {
            name: "role",
            label: "Cargo",
            type: "select",
            value: member.role,
            options: Object.entries(this.roles).map(([id, name]) => ({
              id,
              name,
            })),
          },
          {
            name: "companyId",
            label: "Loja",
            type: "select",
            value: member.company_id,
            options: this.companies(),
            required: false,
          },
        ],
        save: async (v) => {
          const role = v["role"] as Role;
          const companyId = v["companyId"] ? Number(v["companyId"]) : null;
          if (["SELLER", "OWNER"].includes(role) && !companyId)
            throw new Error("Selecione a empresa para este cargo.");
          await firstValueFrom(
            this.admin.membership(member.id, role, companyId),
          );
        },
      },
      () => this.load(this.page()),
    );
  }
}
