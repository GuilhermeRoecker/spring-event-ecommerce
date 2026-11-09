import { expect, test, Page } from "@playwright/test";
import { randomUUID } from "node:crypto";

async function login(page: Page, email: string, password: string) {
  await page.goto("/login");
  await page.getByLabel("E-mail", { exact: true }).fill(email);
  await page.getByLabel("Senha", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Entrar", exact: true }).click();
  await expect(page).toHaveURL(/\/catalog$/);
  const cookie=(await page.context().cookies()).find(cookie => cookie.name === 'JSESSIONID');
  expect(cookie?.httpOnly).toBe(true);
  expect(cookie?.sameSite).toBe('Lax');
}
async function logout(page: Page) {
  await page.getByRole("button", { name: "Abrir menu" }).click();
  await page.getByRole("button", { name: "Sair", exact: true }).click();
  await expect(
    page
      .getByRole("navigation", { name: "Principal" })
      .getByRole("link", { name: "Entrar", exact: true }),
  ).toBeVisible();
}
async function select(page: Page, label: string, option: string) {
  await page.getByLabel(label, { exact: true }).click();
  await page.getByRole("option", { name: option, exact: true }).click();
}
async function saveDialog(page: Page) {
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Salvar", exact: true })
    .click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
}

test("public navigation, private redirect and mobile layout", async ({
  page,
}) => {
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  const response=await page.goto("/catalog");
  expect(response?.headers()['content-security-policy']).toContain("script-src 'self'");
  await expect(
    page.getByRole("heading", { name: "Explore o catálogo" }),
  ).toBeVisible();
  await page
    .getByLabel("Buscar produtos")
    .fill("no-such-product-" + randomUUID());
  await page.getByRole("button", { name: "Buscar", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Nenhum produto encontrado" }),
  ).toBeVisible();
  await page.goto("/checkout");
  await expect(page).toHaveURL(/\/login\?returnUrl=%2Fcheckout$/);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/catalog");
  await expect(
    page.getByRole("heading", { name: "Explore o catálogo" }),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await page.getByRole("button", { name: "Abrir menu" }).click();
  await page.getByRole("button", { name: "Tema escuro", exact: true }).click();
  await expect(page.locator("html")).toHaveClass(/dark/);
  await page.getByRole("button", { name: "Fechar menu", exact: true }).click();
  await page.reload();
  await expect(page.locator("html")).toHaveClass(/dark/);
  await page.screenshot({path: test.info().outputPath('mobile-catalog.png'),fullPage:true});
  expect(errors).toEqual([]);
});

test("real session, management, registration, cart, coupon checkout and delivery", async ({
  page,
  browser,
}) => {
  test.setTimeout(180000);
  const adminEmail = process.env["E2E_ADMIN_EMAIL"];
  const adminPassword = process.env["E2E_ADMIN_PASSWORD"];
  if (!adminEmail || !adminPassword)
    throw new Error(
      "Defina E2E_ADMIN_EMAIL e E2E_ADMIN_PASSWORD de um ambiente exclusivo de testes.",
    );
  const suffix = randomUUID().slice(0, 8),
    shop = "Loja E2E " + suffix,
    category = "Categoria E2E " + suffix,
    product = "Produto E2E " + suffix,
    coupon = "SAVE_" + suffix.toUpperCase();
  const email = `buyer-${suffix}@example.test`,
    password = randomUUID() + "Ab!";
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await login(page, adminEmail, adminPassword);
  await page.goto("/admin/companies");
  await page.getByRole("button", { name: "Nova empresa", exact: true }).click();
  await page.getByLabel("Nome fantasia", { exact: true }).fill(shop);
  await page.getByLabel("Razão social", { exact: true }).fill(shop + " Ltda");
  await saveDialog(page);
  await expect(
    page.getByRole("cell", { name: shop, exact: true }),
  ).toBeVisible();
  await page.goto("/admin/categories");
  await page
    .getByRole("button", { name: "Nova categoria", exact: true })
    .click();
  await page.getByLabel("Nome da categoria", { exact: true }).fill(category);
  await saveDialog(page);
  await page.goto("/admin/products");
  await page.getByRole("button", { name: "Novo produto", exact: true }).click();
  await page.getByLabel("Nome do produto", { exact: true }).fill(product);
  await select(page, "Loja", shop);
  await select(page, "Categoria", category);
  await page
    .getByLabel("Descrição", { exact: true })
    .fill("Descrição preservada no Angular");
  await page.getByLabel("Preço (R$)", { exact: true }).fill("10.50");
  await page.getByLabel("Estoque", { exact: true }).fill("5");
  await saveDialog(page);
  await select(page, "Loja da gestão", shop);
  await expect(
    page.getByRole("cell", { name: product, exact: true }),
  ).toBeVisible();
  await page.goto("/admin/coupons");
  await page.getByRole("button", { name: "Novo cupom", exact: true }).click();
  await page.getByLabel("Código", { exact: true }).fill(coupon);
  await page.getByLabel("Desconto (%)", { exact: true }).fill("15");
  await page.getByLabel("Válido até", { exact: true }).fill("2099-12-31");
  await saveDialog(page);
  await logout(page);
  await page.goto("/login");
  await page.getByRole("button", { name: "Cadastro", exact: true }).click();
  await page
    .getByLabel("Nome completo", { exact: true })
    .fill("Comprador E2E " + suffix);
  await page.getByLabel("E-mail", { exact: true }).fill(email);
  await page.getByLabel("Senha", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Criar conta", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Bem-vindo de volta!" }),
  ).toBeVisible();
  await page.getByLabel("Senha", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Entrar", exact: true }).click();
  await expect(page).toHaveURL(/\/catalog$/);
  await page.goto("/account");
  await page.getByLabel("Rua", { exact: true }).fill("Rua de Teste");
  await page.getByLabel("Número", { exact: true }).fill("42");
  await page.getByLabel("Complemento", { exact: true }).fill("Apto 2");
  await page.getByLabel("Cidade", { exact: true }).fill("Orleans");
  await select(page, "Estado", "SC");
  await page.getByLabel("CEP", { exact: true }).fill("88870000");
  await page
    .getByRole("button", { name: "Adicionar endereço", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Rua de Teste, 42" }),
  ).toBeVisible();
  await page.goto("/catalog");
  await page.getByLabel("Buscar produtos").fill(product);
  await page.getByRole("button", { name: "Buscar", exact: true }).click();
  await page.getByRole("link", { name: product, exact: true }).click();
  await expect(
    page.getByRole("heading", { name: product, exact: true }),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Adicionar ao carrinho", exact: true })
    .click();
  await expect(
    page.getByText("Produto adicionado ao carrinho.", { exact: true }),
  ).toBeVisible();
  await page.goto("/cart");
  await page.getByLabel("Quantidade de " + product, { exact: true }).fill("3");
  await page.getByRole("button", { name: "Atualizar", exact: true }).click();
  await expect(
    page.getByLabel("Quantidade de " + product, { exact: true }),
  ).toHaveValue("3");
  await page
    .getByRole("link", { name: "Continuar para checkout", exact: true })
    .click();
  await page.getByLabel("Cupom de desconto", { exact: true }).fill(coupon);
  await page.getByRole("button", { name: "Criar pedido", exact: true }).click();
  await expect(page).toHaveURL(/\/orders\/\d+$/);
  const orderId = page.url().split("/").pop()!;
  await expect(
    page.getByText("Aguardando pagamento", { exact: true }),
  ).toBeVisible();
  await expect(
    page.getByText("Total: R$ 26,77", { exact: true }),
  ).toBeVisible();
  await expect(
    page.getByText("Rua de Teste, 42, Apto 2, Orleans, SC, 88870000", {
      exact: true,
    }),
  ).toBeVisible();
  await page.reload();
  await expect(
    page.getByRole("heading", { name: "Pedido #" + orderId }),
  ).toBeVisible();
  const adminContext = await browser.newContext();
  const admin = await adminContext.newPage();
  try {
    await login(admin, adminEmail, adminPassword);
    for (const operation of [
      "Confirmar pagamento",
      "Registrar envio",
      "Confirmar entrega",
    ]) {
      await admin.goto("/admin/operations");
      await admin.getByLabel("Número do pedido", { exact: true }).fill(orderId);
      await select(admin, "Operação", operation);
      if (operation !== "Confirmar pagamento") {
        await admin
          .getByLabel("Transportadora", { exact: true })
          .fill("Correios");
        await admin
          .getByLabel("Código de rastreamento", { exact: true })
          .fill("BRTEST123");
      }
      await admin
        .getByRole("button", { name: "Continuar", exact: true })
        .click();
      await admin
        .getByRole("dialog")
        .getByRole("button", { name: "Confirmar", exact: true })
        .click();
      await expect(admin.getByRole("dialog")).toHaveCount(0);
    }
    await page.reload();
    await expect(page.getByText("Entregue", { exact: true })).toHaveCount(2);
    await expect(
      page.getByText("Rastreamento: BRTEST123", { exact: true }),
    ).toBeVisible();
    await admin.goto("/admin/members");
    const member = admin.getByRole("row").filter({ hasText: email });
    await member.getByRole("button", { name: "Alterar acesso" }).click();
    await select(admin, "Cargo", "Vendedor");
    await select(admin, "Loja", shop);
    await saveDialog(admin);
    await page.reload();
    await expect(
      page
        .getByRole("navigation", { name: "Principal" })
        .getByRole("link", { name: "Gestão" }),
    ).toBeVisible();
    await page.goto("/admin/products");
    await expect(
      page.getByRole("cell", { name: product, exact: true }),
    ).toBeVisible();
    await page.goto("/admin/sales");
    await expect(
      page.getByRole("cell", { name: "#" + orderId, exact: true }),
    ).toBeVisible();
    await page.goto("/admin/coupons");
    await expect(page).toHaveURL(/\/admin\/products$/);
    await admin.goto("/admin/coupons");
    const row = admin.getByRole("row").filter({ hasText: coupon });
    await row.getByRole("button", { name: "Desativar", exact: true }).click();
    await admin
      .getByRole("dialog")
      .getByRole("button", { name: "Confirmar", exact: true })
      .click();
    await expect(
      row.getByRole("cell", { name: "Inativo", exact: true }),
    ).toBeVisible();
  } finally {
    await adminContext.close();
  }
  await page.goto("/account");
  const newPassword = randomUUID() + "Ab!";
  await page.getByLabel("Senha atual", { exact: true }).fill(password);
  await page.getByLabel("Nova senha", { exact: true }).fill(newPassword);
  await page
    .getByRole("button", { name: "Alterar senha e sair", exact: true })
    .click();
  await expect(page).toHaveURL(/\/login$/);
  await login(page, email, newPassword);
  expect(errors).toEqual([]);
});
