import { inject, Injectable, signal } from "@angular/core";
import { MatSnackBar } from "@angular/material/snack-bar";
@Injectable({ providedIn: "root" })
export class FeedbackService {
  private readonly snack = inject(MatSnackBar);
  readonly pending = signal(0);
  notify(message: string) {
    this.snack.open(message, "Fechar", {
      duration: 6500,
      horizontalPosition: "center",
    });
  }
  error(error: unknown) {
    this.notify(
      error instanceof Error
        ? error.message
        : "Não foi possível concluir. Tente novamente.",
    );
  }
  async run(work: () => Promise<void>) {
    try {
      await work();
    } catch (error) {
      this.error(error);
    }
  }
}
