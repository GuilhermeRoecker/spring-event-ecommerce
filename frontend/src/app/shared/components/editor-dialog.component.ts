import { Component, inject, signal } from "@angular/core";
import {
  FormControl,
  FormGroup,
  ValidatorFn,
  Validators,
} from "@angular/forms";
import {
  MAT_DIALOG_DATA,
  MatDialogModule,
  MatDialogRef,
} from "@angular/material/dialog";
import { UI } from "../ui";
export type FieldValue = string | number | boolean | null;
export interface EditorField {
  name: string;
  label: string;
  type?:
    | "text"
    | "number"
    | "date"
    | "select"
    | "checkbox"
    | "textarea"
    | "url";
  value?: FieldValue;
  required?: boolean;
  maxLength?: number;
  min?: number;
  max?: number;
  step?: number;
  pattern?: string;
  options?: { id: string | number; name: string }[];
  disabled?: boolean;
}
export interface EditorData {
  title: string;
  description?: string;
  fields: EditorField[];
  submit?: string;
  save: (value: Record<string, FieldValue>) => Promise<void>;
}
@Component({
  selector: "app-editor-dialog",
  imports: [...UI, MatDialogModule],
  templateUrl: "./editor-dialog.component.html",
})
export class EditorDialogComponent {
  readonly data = inject<EditorData>(MAT_DIALOG_DATA);
  readonly dialog = inject(MatDialogRef<EditorDialogComponent, boolean>);
  readonly busy = signal(false);
  readonly error = signal("");
  readonly form: FormGroup<Record<string, FormControl<FieldValue>>>;
  constructor() {
    const controls: Record<string, FormControl<FieldValue>> = {};
    for (const field of this.data.fields) {
      const validators: ValidatorFn[] = [];
      if (field.required !== false && field.type !== "checkbox") {
        validators.push(Validators.required);
        if (!["number", "select"].includes(field.type ?? "text"))
          validators.push(Validators.pattern(/\S/));
      }
      if (field.maxLength !== undefined)
        validators.push(Validators.maxLength(field.maxLength));
      if (field.min !== undefined) validators.push(Validators.min(field.min));
      if (field.max !== undefined) validators.push(Validators.max(field.max));
      if (field.pattern) validators.push(Validators.pattern(field.pattern));
      controls[field.name] = new FormControl<FieldValue>(
        {
          value:
            field.value ??
            (field.type === "checkbox"
              ? true
              : field.type === "select"
                ? null
                : ""),
          disabled: field.disabled ?? false,
        },
        validators,
      );
    }
    this.form = new FormGroup(controls);
  }
  async submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set("");
    this.dialog.disableClose = true;
    try {
      await this.data.save(this.form.getRawValue());
      this.dialog.close(true);
    } catch (error) {
      this.error.set(
        error instanceof Error ? error.message : "Não foi possível salvar.",
      );
    } finally {
      this.busy.set(false);
      this.dialog.disableClose = false;
    }
  }
}
