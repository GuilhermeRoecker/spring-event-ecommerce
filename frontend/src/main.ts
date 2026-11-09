import { bootstrapApplication } from "@angular/platform-browser";
import { AppComponent } from "./app/layout/app.component";
import { appConfig } from "./app/app.config";
bootstrapApplication(AppComponent, appConfig).catch(() => {
  document.body.textContent =
    "Não foi possível iniciar a loja. Atualize a página.";
});
