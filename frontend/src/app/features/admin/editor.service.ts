import { inject, Injectable } from "@angular/core";
import { MatDialog } from "@angular/material/dialog";
import { firstValueFrom } from "rxjs";
import {
  EditorData,
  EditorDialogComponent,
} from "../../shared/components/editor-dialog.component";
import { FeedbackService } from "../../core/services/feedback.service";
@Injectable({ providedIn: "root" })
export class EditorService {
  private readonly dialog = inject(MatDialog);
  private readonly feedback = inject(FeedbackService);
  open(data: EditorData, reload: () => void) {
    void this.feedback.run(async () => {
      const saved = await firstValueFrom(
        this.dialog
          .open<
            EditorDialogComponent,
            EditorData,
            boolean
          >(EditorDialogComponent, { data, width: "620px", maxWidth: "96vw", autoFocus: "first-tabbable" })
          .afterClosed(),
      );
      if (saved) {
        this.feedback.notify("Alterações salvas.");
        reload();
      }
    });
  }
  confirm(
    title: string,
    description: string,
    save: () => Promise<void>,
    reload: () => void,
  ) {
    this.open(
      { title, description, fields: [], submit: "Confirmar", save },
      reload,
    );
  }
}
