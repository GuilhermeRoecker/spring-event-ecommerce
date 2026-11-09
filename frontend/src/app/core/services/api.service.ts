import { inject, Injectable, InjectionToken } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
export const API_URL = new InjectionToken<string>("API_URL", {
  providedIn: "root",
  factory: () => {
    const config = (
      window as Window & { __AROME_CONFIG__?: { apiUrl?: string } }
    ).__AROME_CONFIG__;
    const url = config?.apiUrl ?? "/api";
    if (!url.startsWith("/") || url.startsWith("//"))
      throw new Error("API deve usar a mesma origem");
    return url.replace(/\/$/, "");
  },
});
@Injectable({ providedIn: "root" })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = inject(API_URL);
  get<T>(path: string, params: Record<string, string | number> = {}) {
    return this.http.get<T>(this.base + path, { params });
  }
  post<T>(
    path: string,
    body: unknown = null,
    headers: Record<string, string> = {},
  ) {
    return this.http.post<T>(this.base + path, body, { headers });
  }
  put<T>(path: string, body: unknown) {
    return this.http.put<T>(this.base + path, body);
  }
  delete(path: string) {
    return this.http.delete<void>(this.base + path);
  }
  login(email: string, password: string) {
    return this.http.post<void>(
      this.base + "/auth/login",
      new HttpParams().set("email", email).set("password", password),
    );
  }
}
