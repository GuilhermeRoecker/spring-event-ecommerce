import { inject } from "@angular/core";
import { HttpErrorResponse, HttpInterceptorFn } from "@angular/common/http";
import { Router } from "@angular/router";
import {
  catchError,
  finalize,
  of,
  switchMap,
  throwError,
  Observable,
} from "rxjs";
import { CsrfToken } from "../../shared/models/api.models";
import { API_URL } from "../services/api.service";
import { CsrfService } from "../auth/csrf.service";
import { SessionState } from "../auth/session-state";
import { FeedbackService } from "../services/feedback.service";
export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
  }
}
export const sessionInterceptor: HttpInterceptorFn = (request, next) => {
  const base = inject(API_URL);
  if (!request.url.startsWith(base + "/")) return next(request);
  const csrf = inject(CsrfService),
    session = inject(SessionState),
    router = inject(Router),
    feedback = inject(FeedbackService);
  feedback.pending.update((n) => n + 1);
  const mutation = !["GET", "HEAD", "OPTIONS"].includes(request.method);
  const token$: Observable<CsrfToken | null> = mutation ? csrf.get() : of(null);
  return token$.pipe(
    switchMap((token) =>
      next(
        request.clone({
          withCredentials: true,
          ...(token ? { setHeaders: { [token.headerName]: token.token } } : {}),
        }),
      ),
    ),
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse)) return throwError(() => error);
      if (error.status === 401 && !request.url.endsWith("/auth/login")) {
        session.clear();
        csrf.reset();
        if (!request.url.endsWith("/me"))
          void router.navigate(["/login"], {
            queryParams: { returnUrl: router.url },
          });
      }
      if (error.status === 403) csrf.reset();
      const detail: unknown = error.error?.detail ?? error.error?.error;
      const message =
        typeof detail === "string"
          ? detail
          : error.status === 401
            ? "Entre novamente para continuar."
            : error.status === 403
              ? "Você não tem permissão para esta ação. Se a sessão expirou, entre novamente."
              : error.status === 0
                ? "Não foi possível conectar à loja."
                : "Não foi possível concluir. Confira os dados e tente novamente.";
      return throwError(() => new ApiError(message, error.status));
    }),
    finalize(() => feedback.pending.update((n) => n - 1)),
  );
};
