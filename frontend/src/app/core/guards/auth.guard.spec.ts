import { TestBed } from "@angular/core/testing";
import {
  provideRouter,
  Router,
  RouterStateSnapshot,
  ActivatedRouteSnapshot,
  UrlTree,
} from "@angular/router";
import { signal } from "@angular/core";
import { AuthService } from "../auth/auth.service";
import { Profile } from "../../shared/models/api.models";
import { authGuard, managementGuard, adminGuard } from "./auth.guard";
describe("navigation guards", () => {
  const user = signal<Profile | null>(null);
  beforeEach(() => {
    user.set(null);
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { user } },
      ],
    });
  });
  it("retains a private destination when asking for login", () => {
    const result = TestBed.runInInjectionContext(() =>
      authGuard(
        {} as ActivatedRouteSnapshot,
        { url: "/checkout" } as RouterStateSnapshot,
      ),
    );
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe(
      "/login?returnUrl=%2Fcheckout",
    );
  });
  it("allows authenticated shopping and denies customer management", () => {
    user.set({
      id: 1,
      name: "Pessoa",
      email: "p@test.com",
      role: "CUSTOMER",
      companyId: null,
    });
    expect(
      TestBed.runInInjectionContext(() =>
        authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
      ),
    ).toBe(true);
    expect(
      TestBed.runInInjectionContext(() =>
        managementGuard(
          {} as ActivatedRouteSnapshot,
          {} as RouterStateSnapshot,
        ),
      ),
    ).toBeInstanceOf(UrlTree);
  });
  it("allows sellers into management but not administrative operations", () => {
    user.set({
      id: 1,
      name: "Pessoa",
      email: "p@test.com",
      role: "SELLER",
      companyId: 1,
    });
    expect(
      TestBed.runInInjectionContext(() =>
        managementGuard(
          {} as ActivatedRouteSnapshot,
          {} as RouterStateSnapshot,
        ),
      ),
    ).toBe(true);
    expect(
      TestBed.runInInjectionContext(() =>
        adminGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
      ),
    ).toBeInstanceOf(UrlTree);
  });
});
