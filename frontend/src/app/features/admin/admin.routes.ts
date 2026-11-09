import { Routes } from "@angular/router";
import { adminGuard, ownerGuard } from "../../core/guards/auth.guard";
export const ADMIN_ROUTES: Routes = [
  {
    path: "",
    loadComponent: () =>
      import("./admin.component").then((m) => m.AdminComponent),
    children: [
      { path: "", pathMatch: "full", redirectTo: "products" },
      {
        path: "products",
        loadComponent: () =>
          import("./products.component").then((m) => m.ProductsComponent),
      },
      {
        path: "companies",
        canActivate: [ownerGuard],
        loadComponent: () =>
          import("./companies.component").then((m) => m.CompaniesComponent),
      },
      {
        path: "categories",
        canActivate: [adminGuard],
        loadComponent: () =>
          import("./categories.component").then((m) => m.CategoriesComponent),
      },
      {
        path: "members",
        canActivate: [adminGuard],
        loadComponent: () =>
          import("./members.component").then((m) => m.MembersComponent),
      },
      {
        path: "coupons",
        canActivate: [adminGuard],
        loadComponent: () =>
          import("./coupons.component").then((m) => m.CouponsComponent),
      },
      {
        path: "sales",
        loadComponent: () =>
          import("./sales.component").then((m) => m.SalesComponent),
      },
      {
        path: "operations",
        canActivate: [adminGuard],
        loadComponent: () =>
          import("./operations.component").then((m) => m.OperationsComponent),
      },
    ],
  },
];
