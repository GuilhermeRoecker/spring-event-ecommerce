import { Component, inject, signal } from "@angular/core";
import { FormBuilder, Validators } from "@angular/forms";
import { Router } from "@angular/router";
import { firstValueFrom } from "rxjs";
import { UI } from "../../shared/ui";
import { Address, STATES } from "../../shared/models/api.models";
import { passwordBytes } from "../../shared/validators/password.validator";
import { AuthService } from "../../core/auth/auth.service";
import { FeedbackService } from "../../core/services/feedback.service";
import { AccountService } from "./account.service";
@Component({
  selector: "app-account",
  imports: [...UI],
  templateUrl: "./account.component.html",
})
export class AccountComponent {
  readonly auth = inject(AuthService);
  private readonly service = inject(AccountService);
  private readonly feedback = inject(FeedbackService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  readonly states = STATES;
  readonly addresses = signal<Address[]>([]);
  readonly editing = signal<number | undefined>(undefined);
  readonly busy = signal(false);
  readonly profile = this.fb.nonNullable.group({
    name: [
      this.auth.user()?.name ?? "",
      [
        Validators.required,
        Validators.maxLength(160),
        Validators.pattern(/\S/),
      ],
    ],
  });
  readonly password = this.fb.nonNullable.group({
    currentPassword: ["", Validators.required],
    newPassword: [
      "",
      [Validators.required, Validators.minLength(12), passwordBytes],
    ],
  });
  readonly address = this.fb.nonNullable.group({
    street: [
      "",
      [
        Validators.required,
        Validators.maxLength(200),
        Validators.pattern(/\S/),
      ],
    ],
    number: [
      "",
      [Validators.required, Validators.maxLength(30), Validators.pattern(/\S/)],
    ],
    complement: ["", Validators.maxLength(120)],
    city: [
      "",
      [
        Validators.required,
        Validators.maxLength(120),
        Validators.pattern(/\S/),
      ],
    ],
    state: ["", Validators.required],
    postalCode: ["", [Validators.required, Validators.pattern(/^[0-9]{8}$/)]],
  });
  constructor() {
    this.load();
  }
  private load() {
    void this.feedback.run(async () =>
      this.addresses.set(await firstValueFrom(this.service.addresses())),
    );
  }
  private execute(work: () => Promise<void>) {
    if (this.busy()) return;
    this.busy.set(true);
    void this.feedback.run(async () => {
      try {
        await work();
      } finally {
        this.busy.set(false);
      }
    });
  }
  saveProfile() {
    if (this.profile.invalid) {
      this.profile.markAllAsTouched();
      return;
    }
    this.execute(async () => {
      await this.auth.profile(this.profile.getRawValue().name);
      this.feedback.notify("Seus dados foram atualizados.");
    });
  }
  changePassword() {
    if (this.password.invalid) {
      this.password.markAllAsTouched();
      return;
    }
    this.execute(async () => {
      const p = this.password.getRawValue();
      await this.auth.password(p.currentPassword, p.newPassword);
      this.password.reset();
      await this.router.navigate(["/login"]);
      this.feedback.notify("Senha alterada. Entre novamente.");
    });
  }
  edit(a: Address) {
    this.editing.set(a.id);
    this.address.setValue({
      street: a.street,
      number: a.number,
      complement: a.complement ?? "",
      city: a.city,
      state: a.state,
      postalCode: a.postal_code,
    });
    document
      .getElementById("address-editor")
      ?.scrollIntoView({ block: "start" });
  }
  cancel() {
    this.editing.set(undefined);
    this.address.reset();
  }
  saveAddress() {
    if (this.address.invalid) {
      this.address.markAllAsTouched();
      return;
    }
    this.execute(async () => {
      await firstValueFrom(
        this.service.saveAddress(this.address.getRawValue(), this.editing()),
      );
      this.cancel();
      this.load();
      this.feedback.notify("Endereço salvo.");
    });
  }
  remove(id: number) {
    this.execute(async () => {
      await firstValueFrom(this.service.deleteAddress(id));
      if (this.editing() === id) this.cancel();
      this.load();
      this.feedback.notify("Endereço excluído.");
    });
  }
}
