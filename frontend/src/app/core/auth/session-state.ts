import { Injectable, signal } from "@angular/core";
import { Profile } from "../../shared/models/api.models";
@Injectable({ providedIn: "root" })
export class SessionState {
  readonly user = signal<Profile | null>(null);
  readonly ready = signal(false);
  clear() {
    this.user.set(null);
  }
}
