import { Component, inject, signal } from "@angular/core";
import { FormBuilder, Validators } from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { UI } from "../../shared/ui";
import { AuthService } from "../../core/auth/auth.service";
import { FeedbackService } from "../../core/services/feedback.service";
import { passwordBytes } from "../../shared/validators/password.validator";
@Component({
  selector: "app-authentication",
  imports: [...UI],
  templateUrl: "./authentication.component.html",
})
export class AuthenticationComponent {
  readonly register = signal(false);
  readonly visible = signal(false);
  readonly busy = signal(false);
  readonly toggle = (value: boolean) => !value;
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly feedback = inject(FeedbackService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  readonly form = this.fb.nonNullable.group({
    name: [""],
    email: [
      "",
      [Validators.required, Validators.email, Validators.maxLength(254)],
    ],
    password: ["", Validators.required],
  });
  select(register: boolean) {
    this.register.set(register);
    this.form.controls.name.setValidators(
      register
        ? [
            Validators.required,
            Validators.maxLength(160),
            Validators.pattern(/\S/),
          ]
        : [],
    );
    this.form.controls.password.setValidators(
      register
        ? [Validators.required, Validators.minLength(12), passwordBytes]
        : [Validators.required],
    );
    this.form.controls.name.updateValueAndValidity();
    this.form.controls.password.updateValueAndValidity();
    this.form.controls.password.reset();
    this.form.markAsUntouched();
  }
  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.busy()) return;
    this.busy.set(true);
    void this.feedback.run(async () => {
      try {
        const value = this.form.getRawValue();
        if (this.register()) {
          await this.auth.register(value.name, value.email, value.password);
          this.select(false);
          this.feedback.notify("Conta criada! Entre com seu e-mail e senha.");
        } else {
          await this.auth.login(value.email, value.password);
          this.form.controls.password.reset();
          const target = this.route.snapshot.queryParamMap.get("returnUrl");
          await this.router.navigateByUrl(
            target?.startsWith("/") &&
              !target.startsWith("//") &&
              !target.startsWith("/login")
              ? target
              : "/catalog",
          );
        }
      } finally {
        this.busy.set(false);
      }
    });
  }
}
