import { AbstractControl, ValidationErrors } from "@angular/forms";
export function passwordBytes(
  control: AbstractControl<string>,
): ValidationErrors | null {
  return new TextEncoder().encode(control.value ?? "").length > 72
    ? { passwordBytes: true }
    : null;
}
