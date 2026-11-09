import { TestBed } from "@angular/core/testing";
import {
  HttpClient,
  provideHttpClient,
  withInterceptors,
} from "@angular/common/http";
import {
  provideHttpClientTesting,
  HttpTestingController,
} from "@angular/common/http/testing";
import { provideRouter, Router } from "@angular/router";
import { firstValueFrom } from "rxjs";
import { API_URL } from "../services/api.service";
import { sessionInterceptor, ApiError } from "./session.interceptor";
import { SessionState } from "../auth/session-state";
import { FeedbackService } from "../services/feedback.service";
describe("sessionInterceptor", () => {
  let http: HttpClient, backend: HttpTestingController;
  const pending = { update: vi.fn() };
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([sessionInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: API_URL, useValue: "/api" },
        { provide: FeedbackService, useValue: { pending } },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });
  afterEach(() => backend.verify());
  it("obtains a session token before a mutation and sends cookies", async () => {
    const result = firstValueFrom(http.put("/api/me", { name: "Pessoa" }));
    const token = backend.expectOne("/api/auth/csrf");
    expect(token.request.withCredentials).toBe(true);
    token.flush({ token: "masked", headerName: "X-CSRF-TOKEN" });
    const mutation = backend.expectOne("/api/me");
    expect(mutation.request.headers.get("X-CSRF-TOKEN")).toBe("masked");
    expect(mutation.request.withCredentials).toBe(true);
    mutation.flush({ name: "Pessoa" });
    await result;
  });
  it("shares one CSRF retrieval across concurrent writes", async () => {
    const first = firstValueFrom(http.post("/api/one", {})),
      second = firstValueFrom(http.post("/api/two", {}));
    backend
      .expectOne("/api/auth/csrf")
      .flush({ token: "token", headerName: "X-CSRF-TOKEN" });
    backend.expectOne("/api/one").flush(null);
    backend.expectOne("/api/two").flush(null);
    await Promise.all([first, second]);
  });
  it("clears the current identity on an expired session", async () => {
    const state = TestBed.inject(SessionState);
    state.user.set({
      id: 1,
      name: "Pessoa",
      email: "p@test.com",
      role: "CUSTOMER",
      companyId: null,
    });
    const navigate = vi
      .spyOn(TestBed.inject(Router), "navigate")
      .mockResolvedValue(true);
    const result = firstValueFrom(http.get("/api/me/orders")).catch(
      (error) => error,
    );
    backend
      .expectOne("/api/me/orders")
      .flush({}, { status: 401, statusText: "Unauthorized" });
    expect(await result).toBeInstanceOf(ApiError);
    expect(state.user()).toBeNull();
    expect(navigate).toHaveBeenCalled();
  });
  it("does not replay forbidden mutations and renews the token on a deliberate retry", async () => {
    const result = firstValueFrom(http.post("/api/products", {})).catch(
      (error) => error,
    );
    backend
      .expectOne("/api/auth/csrf")
      .flush({ token: "old", headerName: "X-CSRF-TOKEN" });
    backend
      .expectOne("/api/products")
      .flush(
        { detail: "Acesso negado" },
        { status: 403, statusText: "Forbidden" },
      );
    expect((await result).status).toBe(403);
    backend.expectNone("/api/products");
    const retry = firstValueFrom(http.post("/api/products", {}));
    backend
      .expectOne("/api/auth/csrf")
      .flush({ token: "new", headerName: "X-CSRF-TOKEN" });
    const request = backend.expectOne("/api/products");
    expect(request.request.headers.get("X-CSRF-TOKEN")).toBe("new");
    request.flush(null);
    await retry;
  });
  it("does not send session credentials or tokens to unrelated origins", async () => {
    const result = firstValueFrom(http.get("https://example.com/public"));
    const request = backend.expectOne("https://example.com/public");
    expect(request.request.withCredentials).toBe(false);
    expect(request.request.headers.has("X-CSRF-TOKEN")).toBe(false);
    request.flush({});
    await result;
  });
});
