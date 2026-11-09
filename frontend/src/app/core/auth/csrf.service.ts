import { inject, Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable, shareReplay } from "rxjs";
import { API_URL } from "../services/api.service";
import { CsrfToken } from "../../shared/models/api.models";
@Injectable({ providedIn: "root" })
export class CsrfService {
  private readonly http = inject(HttpClient);
  private readonly base = inject(API_URL);
  private token$?: Observable<CsrfToken>;
  get() {
    return (this.token$ ??= this.http
      .get<CsrfToken>(this.base + "/auth/csrf", { withCredentials: true })
      .pipe(shareReplay({ bufferSize: 1, refCount: false })));
  }
  reset() {
    this.token$ = undefined;
  }
}
