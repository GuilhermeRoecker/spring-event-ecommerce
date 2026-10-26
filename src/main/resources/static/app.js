let user = null,
  csrf = null,
  page = 0,
  checkoutKey = null;
const $ = (id) => document.getElementById(id);
const money = (v) =>
  new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(
    v,
  );
function message(text) {
  $("message").textContent = text;
}
async function token() {
  csrf = await (await fetch("/api/auth/csrf")).json();
}
async function api(path, method = "GET", body, headers = {}) {
  if (method !== "GET" && !csrf) await token();
  const response = await fetch("/api" + path, {
    method,
    headers: {
      ...(method !== "GET" ? { [csrf.headerName]: csrf.token } : {}),
      ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
      ...headers,
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!response.ok) {
    let error;
    try {
      error = await response.json();
    } catch {}
    throw Error(
      response.status === 401
        ? "Entre na sua conta para continuar."
        : error?.detail ||
            error?.error ||
            `Operação recusada (${response.status})`,
    );
  }
  return response.status === 204 ||
    response.headers.get("content-length") === "0"
    ? null
    : await response.text().then((t) => (t ? JSON.parse(t) : null));
}
function card(title, lines = []) {
  const el = document.createElement("article");
  el.className = "card";
  const h = document.createElement("h2");
  h.textContent = title;
  el.append(h);
  for (const text of lines) {
    const p = document.createElement("p");
    p.textContent = text;
    el.append(p);
  }
  return el;
}
function button(parent, label, action) {
  const b = document.createElement("button");
  b.textContent = label;
  b.onclick = () => guard(action);
  parent.append(b);
}
async function guard(action) {
  try {
    await action();
  } catch (e) {
    message(e.message);
  }
}
function data(form) {
  return Object.fromEntries(new FormData(form));
}
async function identify() {
  try {
    user = await api("/me");
  } catch {
    user = null;
  }
  $("identity").textContent = user
    ? `${user.name} • ${user.role}`
    : "Bem-vindo à loja";
  $("logout").hidden = !user;
  $("authforms").hidden = !!user;
  $("profile").hidden = !user;
  $("managebutton").hidden = !user || user.role === "CUSTOMER";
  if (user) $("profileform").elements.name.value = user.name;
}
async function show(view) {
  for (const id of ["catalog", "account", "cart", "orders", "manage"])
    $(id).hidden = id !== view;
  message("");
  if (view === "catalog") await catalog();
  if (view === "account" && user) await addresses();
  if (view === "cart") {
    await cart();
    await addresses();
  }
  if (view === "orders") await orders();
  if (view === "manage") management();
}
async function catalog() {
  const query = new URLSearchParams(data($("search")));
  if (!query.get("categoryId")) query.delete("categoryId");
  query.set("page", page);
  const products = await api("/products?" + query);
  $("products").replaceChildren();
  for (const p of products) {
    const el = card(p.name, [
      p.description,
      money(p.price),
      `Estoque: ${p.stock}`,
    ]);
    if (p.image_url) {
      const img = document.createElement("img");
      img.src = p.image_url;
      img.alt = p.name;
      img.referrerPolicy = "no-referrer";
      el.prepend(img);
    }
    button(el, "Adicionar ao carrinho", async () => {
      const cart = await api("/me/cart");
      const old = cart.find((i) => i.product_id === p.id);
      await api("/me/cart/" + p.id, "PUT", {
        quantity: (old?.quantity || 0) + 1,
      });
      checkoutKey = null;
      message("Produto adicionado.");
    });
    $("products").append(el);
  }
  $("page").textContent = ` Página ${page + 1} `;
  $("previous").disabled = page === 0;
  $("next").disabled = products.length < 20;
}
async function addresses() {
  const rows = await api("/me/addresses");
  $("addresses").replaceChildren();
  $("checkoutaddresses").replaceChildren();
  for (const a of rows) {
    const text = `${a.street}, ${a.number} • ${a.city}/${a.state} • ${a.postal_code}`;
    const el = card(text);
    button(el, "Excluir", async () => {
      await api("/me/addresses/" + a.id, "DELETE");
      await addresses();
    });
    $("addresses").append(el);
    const opt = document.createElement("option");
    opt.value = a.id;
    opt.textContent = text;
    $("checkoutaddresses").append(opt);
  }
}
async function cart() {
  const rows = await api("/me/cart");
  $("cartitems").replaceChildren();
  for (const i of rows) {
    const el = card(i.name, [
      `${money(i.price)} × ${i.quantity} = ${money(i.price * i.quantity)}`,
    ]);
    const n = document.createElement("input");
    n.type = "number";
    n.min = 1;
    n.max = 999;
    n.value = i.quantity;
    n.setAttribute("aria-label", "Quantidade de " + i.name);
    el.append(n);
    button(el, "Atualizar", async () => {
      await api("/me/cart/" + i.product_id, "PUT", {
        quantity: Number(n.value),
      });
      checkoutKey = null;
      await cart();
    });
    button(el, "Remover", async () => {
      await api("/me/cart/" + i.product_id, "DELETE");
      checkoutKey = null;
      await cart();
    });
    $("cartitems").append(el);
  }
}
async function orders() {
  const rows = await api("/me/orders");
  $("orderlist").replaceChildren();
  $("orderdetail").replaceChildren();
  for (const o of rows) {
    const el = card("Pedido #" + o.id, [o.status, money(o.total)]);
    button(el, "Detalhes", async () => {
      const detail = await api("/me/orders/" + o.id);
      const pre = document.createElement("pre");
      pre.textContent = JSON.stringify(detail, null, 2);
      $("orderdetail").replaceChildren(pre);
    });
    $("orderlist").append(el);
  }
}
const operations = {
  "Consultar empresas": { path: "/companies", method: "GET", fields: [] },
  "Consultar categorias": { path: "/categories", method: "GET", fields: [] },
  "Meus produtos": {
    path: "/manage/products",
    method: "GET",
    fields: ["companyId:number"],
  },
  "Criar produto": {
    path: "/products",
    method: "POST",
    fields: [
      "companyId:number",
      "categoryId:number",
      "name",
      "description",
      "price:number",
      "stock:number",
      "active:boolean",
      "imageUrl:optional",
    ],
  },
  "Atualizar produto": {
    path: "/products/{id}",
    method: "PUT",
    fields: [
      "id:number",
      "companyId:number",
      "categoryId:number",
      "name",
      "description",
      "price:number",
      "stock:number",
      "active:boolean",
      "imageUrl:optional",
    ],
  },
  "Desativar produto": {
    path: "/products/{id}",
    method: "DELETE",
    fields: ["id:number"],
  },
  "Vendas da empresa": {
    path: "/manage/order-items",
    method: "GET",
    fields: ["companyId:number"],
  },
  "Atualizar empresa": {
    path: "/companies/{id}",
    method: "PUT",
    fields: ["id:number", "name", "legalName"],
  },
  "Criar empresa": {
    path: "/companies",
    method: "POST",
    admin: true,
    fields: ["name", "legalName"],
  },
  "Criar categoria": {
    path: "/categories",
    method: "POST",
    admin: true,
    fields: ["name"],
  },
  "Listar contas": {
    path: "/admin/accounts",
    method: "GET",
    admin: true,
    fields: [],
  },
  "Alterar cargo e empresa": {
    path: "/admin/accounts/{id}/membership",
    method: "PUT",
    admin: true,
    fields: ["id:number", "role", "companyId:optionalNumber"],
  },
  "Listar cupons": {
    path: "/admin/coupons",
    method: "GET",
    admin: true,
    fields: [],
  },
  "Criar cupom": {
    path: "/admin/coupons",
    method: "POST",
    admin: true,
    fields: [
      "code",
      "percentage:number",
      "expiresOn:date",
      "usageLimit:number",
    ],
  },
  "Desativar cupom": {
    path: "/admin/coupons/{id}",
    method: "DELETE",
    admin: true,
    fields: ["id:number"],
  },
  "Confirmar pagamento": {
    path: "/admin/orders/{id}/confirm-payment",
    method: "POST",
    admin: true,
    fields: ["id:number"],
  },
  "Atualizar entrega": {
    path: "/admin/orders/{id}/delivery",
    method: "PUT",
    admin: true,
    fields: ["id:number", "status", "carrier", "trackingCode"],
  },
};
function management() {
  const select = $("operation");
  select.replaceChildren();
  for (const [name, op] of Object.entries(operations)) {
    if (op.admin && user?.role !== "ADMIN") continue;
    const option = document.createElement("option");
    option.textContent = name;
    select.append(option);
  }
  select.onchange = managementFields;
  managementFields();
  $("managementhelp").textContent =
    "Cargos: CUSTOMER, SELLER, OWNER, ADMIN. Entrega: SHIPPED e depois DELIVERED. Consulte empresas e categorias no catálogo para obter os IDs.";
}
function managementFields() {
  const form = $("managementform");
  form.replaceChildren();
  const op = operations[$("operation").value];
  if (!op) return;
  for (const field of op.fields) {
    const [name, type] = field.split(":");
    const label = document.createElement("label");
    label.textContent = name;
    const input = document.createElement("input");
    input.name = name;
    if (type === "boolean") input.checked = true;
    input.type =
      type === "boolean"
        ? "checkbox"
        : type === "number" || type === "optionalNumber"
          ? "number"
          : type === "date"
            ? "date"
            : "text";
    if (type === "number") input.step = "any";
    input.required = !["boolean", "optional", "optionalNumber"].includes(type);
    label.append(input);
    form.append(label);
  }
  const b = document.createElement("button");
  b.textContent = "Executar";
  form.append(b);
}
$("managementform").onsubmit = (e) => {
  e.preventDefault();
  guard(async () => {
    const op = operations[$("operation").value],
      values = data(e.target);
    for (const field of op.fields) {
      const [name, type] = field.split(":");
      if (type === "number") values[name] = Number(values[name]);
      if (type === "optionalNumber")
        values[name] = values[name] ? Number(values[name]) : null;
      if (type === "boolean") values[name] = e.target.elements[name].checked;
    }
    let path = op.path;
    if (path.includes("{id}")) {
      path = path.replace("{id}", values.id);
      delete values.id;
    }
    if (op.method === "GET") path += "?" + new URLSearchParams(values);
    const result = await api(
      path,
      op.method,
      ["GET", "DELETE"].includes(op.method) ? undefined : values,
    );
    $("managementresult").textContent = JSON.stringify(
      result || { message: "Operação concluída" },
      null,
      2,
    );
  });
};
for (const b of document.querySelectorAll("[data-view]"))
  b.onclick = () => guard(() => show(b.dataset.view));
$("search").onsubmit = (e) => {
  e.preventDefault();
  page = 0;
  guard(catalog);
};
$("previous").onclick = () => {
  page--;
  guard(catalog);
};
$("next").onclick = () => {
  page++;
  guard(catalog);
};
$("login").onsubmit = (e) => {
  e.preventDefault();
  guard(async () => {
    await token();
    const response = await fetch("/api/auth/login", {
      method: "POST",
      headers: {
        [csrf.headerName]: csrf.token,
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body: new URLSearchParams(data(e.target)),
    });
    if (!response.ok) throw Error("Credenciais inválidas");
    e.target.reset();
    await token();
    await identify();
    await show("catalog");
  });
};
$("register").onsubmit = (e) => {
  e.preventDefault();
  guard(async () => {
    await api("/auth/register", "POST", data(e.target));
    e.target.reset();
    message("Conta criada. Entre com seu e-mail e senha.");
  });
};
$("profileform").onsubmit = (e) => {
  e.preventDefault();
  guard(async () => {
    await api("/me", "PUT", data(e.target));
    await identify();
    message("Nome atualizado.");
  });
};
$("passwordform").onsubmit = (e) => {
  e.preventDefault();
  guard(async () => {
    await api("/me/password", "PUT", data(e.target));
    e.target.reset();
    csrf = null;
    await token();
    await identify();
    message("Senha alterada. Entre novamente.");
  });
};
$("addressform").onsubmit = (e) => {
  e.preventDefault();
  guard(async () => {
    await api("/me/addresses", "POST", data(e.target));
    e.target.reset();
    await addresses();
  });
};
$("checkout").onchange = () => {
  checkoutKey = null;
};
$("checkout").onsubmit = (e) => {
  e.preventDefault();
  guard(async () => {
    const body = data(e.target);
    body.addressId = Number(body.addressId);
    checkoutKey ??= crypto.randomUUID();
    const order = await api("/me/checkout", "POST", body, {
      "Idempotency-Key": checkoutKey,
    });
    checkoutKey = null;
    await show("orders");
    message("Pedido #" + order.id + " criado. Aguardando pagamento.");
  });
};
$("logout").onclick = () =>
  guard(async () => {
    await api("/auth/logout", "POST");
    csrf = null;
    await token();
    await identify();
    await show("catalog");
  });
guard(async () => {
  await token();
  await identify();
  const categories = await api("/categories");
  for (const c of categories) {
    const o = document.createElement("option");
    o.value = c.id;
    o.textContent = c.name;
    $("categories").append(o);
  }
  await catalog();
});
