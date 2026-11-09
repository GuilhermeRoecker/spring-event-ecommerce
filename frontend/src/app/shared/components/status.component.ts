import { Component, input, computed } from "@angular/core";
import { STATUS_NAMES } from "../models/api.models";
@Component({ selector: "app-status", templateUrl: "./status.component.html" })
export class StatusComponent {
  readonly value = input.required<string>();
  readonly label = computed(() => STATUS_NAMES[this.value()] ?? this.value());
}
