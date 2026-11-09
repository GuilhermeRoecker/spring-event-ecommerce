import { Component, inject, signal } from "@angular/core";
import {
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from "@angular/router";
import { MatButtonModule } from "@angular/material/button";
import { MatToolbarModule } from "@angular/material/toolbar";
import { MatProgressBarModule } from "@angular/material/progress-bar";
import { MatSidenavModule } from "@angular/material/sidenav";
import { AuthService } from "../core/auth/auth.service";
import { FeedbackService } from "../core/services/feedback.service";
@Component({
  selector: "app-root",
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatButtonModule,
    MatToolbarModule,
    MatProgressBarModule,
    MatSidenavModule,
  ],
  templateUrl: "./app.component.html",
})
export class AppComponent {
  readonly auth = inject(AuthService);
  readonly feedback = inject(FeedbackService);
  private readonly router = inject(Router);
  readonly dark = signal(false);
  constructor() {
    try {
      this.dark.set(localStorage.getItem("arome-theme") === "dark");
    } catch {}
    this.applyTheme();
  }
  toggleTheme() {
    this.dark.update((value) => !value);
    this.applyTheme();
    try {
      localStorage.setItem("arome-theme", this.dark() ? "dark" : "light");
    } catch {}
  }
  private applyTheme() {
    document.documentElement.classList.toggle("dark", this.dark());
  }
  logout() {
    void this.feedback.run(async () => {
      await this.auth.logout();
      await this.router.navigate(["/catalog"]);
      this.feedback.notify("Você saiu da sua conta.");
    });
  }
}
