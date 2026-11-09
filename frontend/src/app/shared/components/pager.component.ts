import { Component, input, output } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
@Component({
  selector: "app-pager",
  imports: [MatButtonModule],
  templateUrl: "./pager.component.html",
})
export class PagerComponent {
  readonly page = input(0);
  readonly hasNext = input(false);
  readonly change = output<number>();
}
