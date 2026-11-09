import { Component, inject } from "@angular/core";
import { FormBuilder, Validators } from "@angular/forms";
import { firstValueFrom } from "rxjs";
import { UI } from "../../shared/ui";
import { AdminService } from "./admin.service";
import { EditorService } from "./editor.service";
@Component({
  selector: "app-operations",
  imports: [...UI],
  templateUrl: "./operations.component.html",
})
export class OperationsComponent {
  private readonly admin = inject(AdminService);
  private readonly editor = inject(EditorService);
  readonly form = inject(FormBuilder).nonNullable.group({
    id: [
      1,
      [Validators.required, Validators.min(1), Validators.pattern(/^[0-9]+$/)],
    ],
    operation: ["PAY" as "PAY" | "SHIPPED" | "DELIVERED"],
    carrier: [""],
    trackingCode: [""],
  });
  validate() {
    const validators =
      this.form.controls.operation.value === "PAY"
        ? []
        : [
            Validators.required,
            Validators.maxLength(120),
            Validators.pattern(/\S/),
          ];
    for (const control of [
      this.form.controls.carrier,
      this.form.controls.trackingCode,
    ]) {
      control.setValidators(validators);
      control.updateValueAndValidity();
    }
  }
  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.editor.confirm(
      "Confirmar operação?",
      "Pedido #" +
        v.id +
        ": " +
        {
          PAY: "confirmar pagamento verificado",
          SHIPPED: "registrar envio",
          DELIVERED: "confirmar entrega",
        }[v.operation] +
        ".",
      async () => {
        if (v.operation === "PAY")
          await firstValueFrom(this.admin.confirmPayment(v.id));
        else
          await firstValueFrom(
            this.admin.track(v.id, v.operation, v.carrier, v.trackingCode),
          );
      },
      () => this.form.reset(),
    );
  }
}
