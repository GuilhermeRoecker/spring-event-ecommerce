import { inject } from "@angular/core";
import { CanActivateFn, Router } from "@angular/router";
import { AuthService } from "../auth/auth.service";
export const authGuard: CanActivateFn = (_, state) =>
  inject(AuthService).user()
    ? true
    : inject(Router).createUrlTree(["/login"], {
        queryParams: { returnUrl: state.url },
      });
export const managementGuard: CanActivateFn = () => {
  const user = inject(AuthService).user();
  return user && user.role !== "CUSTOMER"
    ? true
    : inject(Router).createUrlTree(["/catalog"]);
};
export const adminGuard: CanActivateFn = () =>
  inject(AuthService).user()?.role === "ADMIN"
    ? true
    : inject(Router).createUrlTree(["/admin/products"]);
export const ownerGuard: CanActivateFn = () =>
  ["ADMIN", "OWNER"].includes(inject(AuthService).user()?.role ?? "")
    ? true
    : inject(Router).createUrlTree(["/admin/products"]);
