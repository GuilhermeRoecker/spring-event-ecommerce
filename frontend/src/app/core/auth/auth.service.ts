import { inject, Injectable } from "@angular/core";
import { catchError, firstValueFrom, of, tap } from "rxjs";
import { ApiService } from "../services/api.service";
import { SessionState } from "./session-state";
import { CsrfService } from "./csrf.service";
import { Profile } from "../../shared/models/api.models";
import { ApiError } from "../interceptors/session.interceptor";
@Injectable({ providedIn: "root" })
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly state = inject(SessionState);
  private readonly csrf = inject(CsrfService);
  readonly user = this.state.user.asReadonly();
  async initialize() {
    try {
      await firstValueFrom(
        this.api.get<Profile>("/me").pipe(
          tap((user) => this.state.user.set(user)),
          catchError((error: unknown) => {
            if (error instanceof ApiError && error.status === 401)
              return of(null);
            throw error;
          }),
        ),
      );
    } finally {
      this.state.ready.set(true);
    }
  }
  async login(email: string, password: string) {
    await firstValueFrom(this.api.login(email, password));
    this.csrf.reset();
    await this.refresh();
  }
  async register(name: string, email: string, password: string) {
    await firstValueFrom(
      this.api.post("/auth/register", { name, email, password }),
    );
  }
  async refresh() {
    this.state.user.set(await firstValueFrom(this.api.get<Profile>("/me")));
  }
  async logout() {
    await firstValueFrom(this.api.post("/auth/logout"));
    this.state.clear();
    this.csrf.reset();
  }
  async password(currentPassword: string, newPassword: string) {
    await firstValueFrom(
      this.api.put("/me/password", { currentPassword, newPassword }),
    );
    this.state.clear();
    this.csrf.reset();
  }
  async profile(name: string) {
    this.state.user.set(
      await firstValueFrom(this.api.put<Profile>("/me", { name })),
    );
  }
}
