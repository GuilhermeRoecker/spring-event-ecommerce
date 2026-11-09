import { FormControl } from "@angular/forms";
import { passwordBytes } from "./password.validator";
describe("passwordBytes", () => {
  it("accepts exactly 72 UTF-8 bytes", () => {
    expect(
      passwordBytes(new FormControl("é".repeat(36), { nonNullable: true })),
    ).toBeNull();
  });
  it("rejects fewer than 72 characters when UTF-8 exceeds the BCrypt byte limit", () => {
    expect(
      passwordBytes(new FormControl("é".repeat(40), { nonNullable: true })),
    ).toEqual({ passwordBytes: true });
  });
});
