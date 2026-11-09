import { inject, Injectable } from "@angular/core";
import { ApiService } from "../../core/services/api.service";
import { Address, AddressInput } from "../../shared/models/api.models";
@Injectable({ providedIn: "root" })
export class AccountService {
  private readonly api = inject(ApiService);
  addresses() {
    return this.api.get<Address[]>("/me/addresses");
  }
  saveAddress(input: AddressInput, id?: number) {
    return id
      ? this.api.put<void>("/me/addresses/" + id, input)
      : this.api.post<void>("/me/addresses", input);
  }
  deleteAddress(id: number) {
    return this.api.delete("/me/addresses/" + id);
  }
}
