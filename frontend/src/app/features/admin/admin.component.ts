import { Component, inject } from "@angular/core";
import { RouterOutlet, RouterLink, RouterLinkActive } from "@angular/router";
import { MatButtonModule } from "@angular/material/button";
import { AuthService } from "../../core/auth/auth.service";
import { ROLE_NAMES } from "../../shared/models/api.models";
@Component({
  selector: "app-admin",
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatButtonModule],
  templateUrl: "./admin.component.html",
})
export class AdminComponent {
  readonly auth = inject(AuthService);
  readonly roles = ROLE_NAMES;
}
