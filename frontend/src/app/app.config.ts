import {
  ApplicationConfig,
  inject,
  LOCALE_ID,
  provideAppInitializer,
} from "@angular/core";
import { provideHttpClient, withInterceptors } from "@angular/common/http";
import { provideRouter, withComponentInputBinding } from "@angular/router";
import { registerLocaleData } from "@angular/common";
import pt from "@angular/common/locales/pt";
import { routes } from "./app.routes";
import { sessionInterceptor } from "./core/interceptors/session.interceptor";
import { AuthService } from "./core/auth/auth.service";
import { FeedbackService } from "./core/services/feedback.service";
registerLocaleData(pt);
export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([sessionInterceptor])),
    { provide: LOCALE_ID, useValue: "pt-BR" },
    provideAppInitializer(() => {
      const feedback = inject(FeedbackService);
      return inject(AuthService)
        .initialize()
        .catch((error) => feedback.error(error));
    }),
  ],
};
