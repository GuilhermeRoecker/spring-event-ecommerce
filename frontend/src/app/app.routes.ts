import { Routes } from "@angular/router";
import { authGuard, managementGuard } from "./core/guards/auth.guard";
export const routes: Routes = [
  { path: "", pathMatch: "full", redirectTo: "catalog" },
  {
    path: "catalog",
    title: "Produtos · Arôme",
    loadComponent: () =>
      import("./features/catalog/catalog.component").then(
        (m) => m.CatalogComponent,
      ),
  },
  {
    path: "products/:id",
    title: "Seu achado · Arôme",
    loadComponent: () =>
      import("./features/catalog/product-detail.component").then(
        (m) => m.ProductDetailComponent,
      ),
  },
  {
    path: "login",
    title: "Sua conta · Arôme",
    loadComponent: () =>
      import("./features/authentication/authentication.component").then(
        (m) => m.AuthenticationComponent,
      ),
  },
  {
    path: "account",
    canActivate: [authGuard],
    title: "Minha conta · Arôme",
    loadComponent: () =>
      import("./features/account/account.component").then(
        (m) => m.AccountComponent,
      ),
  },
  {
    path: "cart",
    canActivate: [authGuard],
    title: "Carrinho · Arôme",
    loadComponent: () =>
      import("./features/cart/cart.component").then((m) => m.CartComponent),
  },
  {
    path: "checkout",
    canActivate: [authGuard],
    title: "Checkout · Arôme",
    loadComponent: () =>
      import("./features/checkout/checkout.component").then(
        (m) => m.CheckoutComponent,
      ),
  },
  {
    path: "orders",
    canActivate: [authGuard],
    title: "Meus pedidos · Arôme",
    loadComponent: () =>
      import("./features/orders/orders.component").then(
        (m) => m.OrdersComponent,
      ),
  },
  {
    path: "orders/:id",
    canActivate: [authGuard],
    title: "Pedido · Arôme",
    loadComponent: () =>
      import("./features/orders/order-detail.component").then(
        (m) => m.OrderDetailComponent,
      ),
  },
  {
    path: "admin",
    canActivate: [authGuard, managementGuard],
    title: "Gestão · Arôme",
    loadChildren: () =>
      import("./features/admin/admin.routes").then((m) => m.ADMIN_ROUTES),
  },
  { path: "**", redirectTo: "catalog" },
];
