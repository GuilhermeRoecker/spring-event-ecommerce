import { Component, inject, signal } from "@angular/core";
import { firstValueFrom } from "rxjs";
import { UI } from "../../shared/ui";
import { Category } from "../../shared/models/api.models";
import { CatalogService } from "../catalog/catalog.service";
import { FeedbackService } from "../../core/services/feedback.service";
import { AdminService } from "./admin.service";
import { EditorService } from "./editor.service";
@Component({
  selector: "app-categories",
  imports: [...UI],
  templateUrl: "./categories.component.html",
})
export class CategoriesComponent {
  private readonly catalog = inject(CatalogService);
  private readonly admin = inject(AdminService);
  private readonly editor = inject(EditorService);
  private readonly feedback = inject(FeedbackService);
  readonly categories = signal<Category[]>([]);
  constructor() {
    this.load();
  }
  load() {
    void this.feedback.run(async () =>
      this.categories.set(await firstValueFrom(this.catalog.categories())),
    );
  }
  edit(category?: Category) {
    this.editor.open(
      {
        title: category ? "Editar categoria" : "Nova categoria",
        fields: [
          {
            name: "name",
            label: "Nome da categoria",
            value: category?.name,
            maxLength: 120,
          },
        ],
        save: async (value) => {
          await firstValueFrom(
            this.admin.category(String(value["name"]), category?.id),
          );
        },
      },
      () => this.load(),
    );
  }
  remove(category: Category) {
    this.editor.confirm(
      "Excluir categoria?",
      category.name +
        " só pode ser excluída se não estiver vinculada a produtos.",
      async () => {
        await firstValueFrom(this.admin.deleteCategory(category.id));
      },
      () => this.load(),
    );
  }
}
