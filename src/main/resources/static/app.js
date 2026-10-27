import {
  $,
  node,
  icon,
  hydrateIcons,
  action,
  badge,
  statusBadge,
  empty,
  image,
  table,
  options,
  money,
  date,
  roles,
} from "/assets/js/ui.js";
import { createManagement } from "/assets/js/management.js";

let user = null,
  csrf = null,
  page = 0,
  ordersPage = 0,
  checkoutKey = null,
  toastTimer,
  returnTo = "catalog";
let categories = [],
  companies = [],
  catalogVersion = 0;
const views = ["catalog", "account", "cart", "orders", "manage"];
function notify(text, error = false) {
  const el = $("message");
  clearTimeout(toastTimer);
  el.textContent = text;
  el.hidden = !text;
  el.classList.toggle("toast-error", error);
  if (text)
    toastTimer = setTimeout(
      () => {
        el.hidden = true;
      },
      error ? 9000 : 6000,
    );
}
async function token() {
  const response = await fetch("/api/auth/csrf");
  if (!response.ok)
    throw Error("Não foi possível conectar à loja. Tente novamente.");
  csrf = await response.json();
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
    if (response.status === 401) {
      if (user) {
        user = null;
        csrf = null;
        management.reset();
        renderIdentity();
        authTab("login");
        navigate("account");
      }
      throw Error("Sua sessão terminou. Entre na sua conta para continuar.");
    }
    throw Error(
      error?.detail ||
        error?.error ||
        (response.status === 403
          ? "Você não tem permissão para esta ação."
          : "Não foi possível concluir. Tente novamente."),
    );
  }
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}
async function guard(fn) {
  try {
    await fn();
  } catch (error) {
    notify(error.message, true);
  }
}
function act(label, fn, config = {}) {
  const b = action(label, () => {}, config);
  b.onclick = async () => {
    b.disabled = true;
    try {
      await fn();
    } catch (error) {
      notify(error.message, true);
    } finally {
      b.disabled = config.disabled ?? false;
    }
  };
  return b;
}
function values(form) {
  return Object.fromEntries(new FormData(form));
}
function submit(id, fn) {
  $(id).onsubmit = async (e) => {
    e.preventDefault();
    const form = e.currentTarget,
      button = form.querySelector("button[type=submit],button:not([type])");
    if (button.disabled) return;
    button.disabled = true;
    button.setAttribute("aria-busy", "true");
    try {
      await fn(form);
    } catch (error) {
      notify(error.message, true);
    } finally {
      button.disabled = false;
      button.removeAttribute("aria-busy");
    }
  };
}
function closeDialogs() {
  for (const dialog of document.querySelectorAll("dialog[open]"))
    dialog.close();
}
function navigate(view) {
  closeDialogs();
  if (location.hash === "#" + view) guard(() => show(view));
  else location.hash = view;
}
function authTab(tab) {
  const registering = tab === "register";
  $("login").hidden = registering;
  $("register").hidden = !registering;
  for (const name of ["login", "register"]) {
    const button = $("tab-" + name),
      active = name === tab;
    button.setAttribute("aria-selected", String(active));
    button.tabIndex = active ? 0 : -1;
  }
}
function applyTheme(theme) {
  document.documentElement.dataset.theme = theme;
  for (const b of document.querySelectorAll(".theme-button")) {
    b.replaceChildren(icon(theme === "dark" ? "sun" : "moon"));
    b.setAttribute(
      "aria-label",
      theme === "dark" ? "Ativar tema claro" : "Ativar tema escuro",
    );
  }
  try {
    localStorage.setItem("arome-theme", theme);
  } catch {}
}
function initialTheme() {
  try {
    return (
      localStorage.getItem("arome-theme") ||
      (matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light")
    );
  } catch {
    return "light";
  }
}
async function identify() {
  try {
    user = await api("/me");
  } catch {
    user = null;
  }
  renderIdentity();
  await updateCartCount();
}
function renderIdentity() {
  if (!user) {
    $("cartcount").textContent = "0";
    $("cartcount").hidden = true;
  }
  $("identity").textContent = user
    ? `Olá, ${user.name.split(" ")[0]}`
    : "Olá, visitante";
  $("accountlabel").textContent = user ? "Minha conta" : "Login / Cadastro";
  $("logout").hidden = !user;
  $("authforms").hidden = !!user;
  $("profile").hidden = !user;
  $("managebutton").hidden = !user || user.role === "CUSTOMER";
  if (user) {
    $("profileform").elements.name.value = user.name;
    $("profileemail").textContent = user.email;
  }
}
async function updateCartCount() {
  let count = 0;
  if (user) {
    try {
      const items = await api("/me/cart");
      count = items.reduce((sum, p) => sum + p.quantity, 0);
    } catch {}
  }
  $("cartcount").textContent = count;
  $("cartcount").hidden = count === 0;
}
async function refreshLookups() {
  [categories, companies] = await Promise.all([
    api("/categories"),
    api("/companies"),
  ]);
  const category = $("categories").value,
    company = $("stores").value;
  options($("categories"), categories, "Todas as categorias");
  options($("stores"), companies, "Todas as lojas");
  $("categories").value = categories.some((c) => String(c.id) === category)
    ? category
    : "";
  $("stores").value = companies.some((c) => String(c.id) === company)
    ? company
    : "";
}
const management = createManagement({
  api,
  getUser: () => user,
  notify,
  refreshLookups,
});
async function show(view) {
  if (!views.includes(view)) view = "catalog";
  if (["cart", "orders", "manage"].includes(view) && !user) {
    returnTo = view;
    location.replace("#account");
    notify("Entre na sua conta para continuar.");
    return;
  }
  for (const id of views) $(id).hidden = id !== view;
  document.body.classList.toggle("auth-mode", view === "account" && !user);
  document.title =
    "Arôme • " +
    {
      catalog: "Produtos",
      account: user ? "Minha conta" : "Login",
      cart: "Carrinho",
      orders: "Meus pedidos",
      manage: "Gestão",
    }[view];
  for (const b of document.querySelectorAll("[data-view]")) {
    if (b.dataset.view === view) b.setAttribute("aria-current", "page");
    else b.removeAttribute("aria-current");
  }
  window.scrollTo({ top: 0, behavior: "instant" });
  if (view === "catalog") await catalog();
  if (view === "account" && user) await addresses();
  if (view === "cart") await Promise.all([cart(), addresses()]);
  if (view === "orders") await orders();
  if (view === "manage") await management.open();
}
function productSymbol(p) {
  const name =
    categories.find((c) => c.id === p.category_id)?.name.toLowerCase() || "";
  return name.includes("livro")
    ? "book"
    : name.includes("eletr")
      ? "monitor"
      : name.includes("casa")
        ? "home"
        : name.includes("acess")
          ? "tag"
          : "box";
}
async function addToCart(p) {
  if (!user) {
    returnTo = "catalog";
    navigate("account");
    notify("Entre na sua conta para adicionar produtos.");
    return;
  }
  const items = await api("/me/cart"),
    old = items.find((i) => i.product_id === p.id);
  await api("/me/cart/" + p.id, "PUT", { quantity: (old?.quantity || 0) + 1 });
  checkoutKey = null;
  await updateCartCount();
  notify("Produto adicionado ao carrinho.");
}
function productCard(p) {
  const el = node("article", "product-card"),
    visual = image(p, "product-image", productSymbol(p));
  if (p.stock === 0) visual.append(badge("Sem estoque", "neutral"));
  else if (p.stock === 1) visual.append(badge("Última unidade", "warning"));
  const info = node("div", "product-info");
  info.append(
    node(
      "span",
      "product-company",
      companies.find((c) => c.id === p.company_id)?.name || "Arôme",
    ),
    node("h2", "", p.name),
    node("p", "product-description", p.description),
    node("p", "product-price", money(p.price)),
    node(
      "span",
      "stock-label",
      p.stock > 0
        ? `${p.stock} ${p.stock === 1 ? "unidade disponível" : "unidades disponíveis"}`
        : "Produto sem estoque no momento.",
    ),
  );
  const buttons = node("div", "product-actions");
  buttons.append(
    act(
      p.stock ? "Adicionar ao carrinho" : "Indisponível",
      () => addToCart(p),
      { symbol: "cart", disabled: p.stock === 0 },
    ),
    act("", () => productDetail(p), {
      symbol: "eye",
      className: "icon-button",
    }),
  );
  buttons.lastChild.setAttribute("aria-label", "Ver detalhes de " + p.name);
  info.append(buttons);
  el.append(visual, info);
  return el;
}
async function catalog() {
  const stamp = ++catalogVersion,
    query = new URLSearchParams(values($("search")));
  for (const key of ["categoryId", "companyId"])
    if (!query.get(key)) query.delete(key);
  query.set("page", page);
  $("catalogcount").textContent = "Carregando produtos…";
  try {
    const products = await api("/products?" + query);
    if (stamp !== catalogVersion) return;
    $("products").replaceChildren(...products.map(productCard));
    if (!products.length)
      $("products").append(
        empty(
          "Nenhum produto encontrado",
          "Experimente outro termo ou escolha uma categoria diferente.",
          "search",
        ),
      );
    $("catalogcount").textContent =
      `${products.length} produto${products.length === 1 ? "" : "s"} nesta página`;
    $("page").textContent = `Página ${page + 1}`;
    $("previous").disabled = page === 0;
    $("next").disabled = products.length < 20;
  } catch (error) {
    $("catalogcount").textContent = "Não foi possível carregar os produtos.";
    throw error;
  }
}
function productDetail(p) {
  $("producttitle").textContent = p.name;
  const content = $("productdetail");
  content.replaceChildren(
    image(p, "product-image details-image", productSymbol(p)),
    node(
      "p",
      "product-company",
      companies.find((c) => c.id === p.company_id)?.name || "Arôme",
    ),
    node("p", "detail-description", p.description),
    node("p", "detail-price", money(p.price)),
    act(
      p.stock ? "Adicionar ao carrinho" : "Indisponível",
      async () => {
        $("productdialog").close();
        await addToCart(p);
      },
      { symbol: "cart", className: "full-width", disabled: p.stock === 0 },
    ),
  );
  $("productdialog").showModal();
}
async function addresses() {
  const rows = await api("/me/addresses");
  $("addresses").replaceChildren();
  const previous = $("checkoutaddresses").value;
  options(
    $("checkoutaddresses"),
    rows.map((a) => ({
      id: a.id,
      name: `${a.street}, ${a.number} • ${a.city}/${a.state}`,
    })),
    rows.length ? undefined : "Cadastre um endereço na sua conta",
  );
  if (rows.some((a) => String(a.id) === previous))
    $("checkoutaddresses").value = previous;
  for (const a of rows) {
    const el = node("article", "panel address-card");
    el.append(
      icon("pin"),
      node("h3", "", `${a.street}, ${a.number}`),
      node("p", "", `${a.city} / ${a.state}`),
      node("p", "", `CEP ${a.postal_code}`),
    );
    if (a.complement) el.append(node("p", "", a.complement));
    el.append(
      act(
        "Excluir endereço",
        async () => {
          await api("/me/addresses/" + a.id, "DELETE");
          checkoutKey = null;
          await addresses();
          notify("Endereço excluído.");
        },
        { className: "text-button" },
      ),
    );
    $("addresses").append(el);
  }
  if (!rows.length)
    $("addresses").append(
      empty(
        "Sua entrega começa aqui",
        "Adicione um endereço para finalizar suas compras.",
        "pin",
      ),
    );
}
async function cart() {
  const rows = await api("/me/cart");
  $("cartitems").replaceChildren();
  let total = 0;
  for (const i of rows) {
    total += Number(i.price) * i.quantity;
    const el = node("article", "panel cart-item"),
      details = node("div", "cart-item-details");
    details.append(
      node("h2", "", i.name),
      node("p", "", money(i.price) + " por unidade"),
    );
    if (!i.active || i.stock < i.quantity)
      details.append(badge("Revise a disponibilidade", "warning"));
    const controls = node("div", "quantity-control"),
      n = node("input");
    n.type = "number";
    n.min = 1;
    n.max = 999;
    n.step = 1;
    n.value = i.quantity;
    n.setAttribute("aria-label", "Quantidade de " + i.name);
    controls.append(
      n,
      act(
        "Atualizar",
        async () => {
          if (!n.checkValidity()) {
            n.reportValidity();
            return;
          }
          await api("/me/cart/" + i.product_id, "PUT", {
            quantity: Number(n.value),
          });
          checkoutKey = null;
          await Promise.all([cart(), updateCartCount()]);
        },
        { className: "button-outline" },
      ),
      act(
        "Remover",
        async () => {
          await api("/me/cart/" + i.product_id, "DELETE");
          checkoutKey = null;
          await Promise.all([cart(), updateCartCount()]);
        },
        { className: "text-button" },
      ),
    );
    details.append(controls);
    el.append(
      image(i),
      details,
      node("strong", "item-total", money(i.price * i.quantity)),
    );
    $("cartitems").append(el);
  }
  if (!rows.length) {
    const el = empty(
      "Seu carrinho está esperando",
      "Que tal encontrar seu próximo achado?",
      "cart",
    );
    el.append(act("Explorar produtos", () => navigate("catalog")));
    $("cartitems").append(el);
  }
  $("carttotal").textContent = money(total);
  $("checkoutsubmit").disabled = rows.length === 0;
}
async function orders() {
  const rows = await api("/me/orders?page=" + ordersPage);
  $("orderlist").replaceChildren();
  $("orderdetail").replaceChildren();
  for (const o of rows) {
    const el = node("article", "panel order-card"),
      top = node("div", "order-top");
    top.append(node("h2", "", "Pedido #" + o.id), icon("box"));
    el.append(
      top,
      node("time", "", date(o.created_at)),
      node("p", "order-total", money(o.total)),
      statusBadge(o.status),
      act("Ver detalhes", () => orderDetail(o.id), {
        className: "button-outline",
      }),
    );
    $("orderlist").append(el);
  }
  if (!rows.length)
    $("orderlist").append(
      empty(
        "Seus achados vão aparecer aqui",
        "Assim que você fizer uma compra, poderá acompanhar tudo por aqui.",
        "box",
      ),
    );
  $("orderspage").textContent = `Página ${ordersPage + 1}`;
  $("ordersprevious").disabled = ordersPage === 0;
  $("ordersnext").disabled = rows.length < 50;
}
async function orderDetail(id) {
  const detail = await api("/me/orders/" + id),
    { order, items, payment, delivery } = detail;
  const panel = node("article", "panel order-detail"),
    heading = node("div", "page-heading");
  heading.append(
    node("h2", "", "Detalhes do pedido #" + id),
    statusBadge(order.status),
  );
  panel.append(
    heading,
    table(
      ["Produto", "Quantidade", "Preço unitário", "Total"],
      items.map((i) => [
        i.name,
        i.quantity,
        money(i.unit_price),
        money(i.quantity * i.unit_price),
      ]),
    ),
  );
  const grid = node("div", "detail-grid"),
    address = node("div"),
    summary = node("div");
  address.append(
    node("h3", "", "Endereço de entrega"),
    node("p", "muted", order.shipping_address),
    node("h3", "", "Acompanhamento"),
    node("p", "", statusesText(delivery.status)),
  );
  if (delivery.carrier) address.append(node("p", "", delivery.carrier));
  if (delivery.tracking_code)
    address.append(node("p", "", "Rastreamento: " + delivery.tracking_code));
  summary.append(
    node("h3", "", "Pagamento"),
    node(
      "p",
      "",
      ({ PIX: "Pix", CARD: "Cartão", BOLETO: "Boleto" }[payment.method] ||
        payment.method) +
        " • " +
        statusesText(payment.status),
    ),
    node("p", "", "Subtotal: " + money(order.subtotal)),
    node("p", "", "Desconto: " + money(order.discount)),
    node("strong", "", "Total: " + money(order.total)),
  );
  grid.append(address, summary);
  panel.append(grid);
  $("orderdetail").replaceChildren(panel);
  panel.scrollIntoView({
    behavior: matchMedia("(prefers-reduced-motion: reduce)").matches
      ? "instant"
      : "smooth",
    block: "start",
  });
}
function statusesText(status) {
  return (
    {
      AWAITING_PAYMENT: "Aguardando pagamento",
      PENDING: "Pendente",
      PAID: "Pago",
      SHIPPED: "Enviado",
      DELIVERED: "Entregue",
    }[status] || status
  );
}

hydrateIcons();
applyTheme(initialTheme());
for (const b of document.querySelectorAll(".theme-button"))
  b.onclick = () =>
    applyTheme(
      document.documentElement.dataset.theme === "dark" ? "light" : "dark",
    );
for (const b of document.querySelectorAll("[data-view]"))
  b.onclick = () => navigate(b.dataset.view);
for (const b of document.querySelectorAll(".close-dialog"))
  b.onclick = () => b.closest("dialog").close();
for (const dialog of document.querySelectorAll("dialog"))
  dialog.addEventListener("click", (e) => {
    if (e.target !== dialog) return;
    const rect = dialog.getBoundingClientRect();
    if (
      e.clientX < rect.left ||
      e.clientX > rect.right ||
      e.clientY < rect.top ||
      e.clientY > rect.bottom
    )
      dialog.close();
  });
$("menubutton").onclick = () => $("menudialog").showModal();
$("menutheme").onclick = () =>
  applyTheme(
    document.documentElement.dataset.theme === "dark" ? "light" : "dark",
  );
$("tab-login").onclick = () => authTab("login");
$("tab-register").onclick = () => authTab("register");
$("registershortcut").onclick = () => authTab("register");
$("loginshortcut").onclick = () => authTab("login");
for (const name of ["login", "register"])
  $("tab-" + name).addEventListener("keydown", (e) => {
    if (["ArrowLeft", "ArrowRight", "Home", "End"].includes(e.key)) {
      e.preventDefault();
      const next =
        e.key === "Home"
          ? "login"
          : e.key === "End"
            ? "register"
            : name === "login"
              ? "register"
              : "login";
      authTab(next);
      $("tab-" + next).focus();
    }
  });
for (const b of document.querySelectorAll("[data-password]"))
  b.onclick = () => {
    const input = $(b.dataset.password).elements.password;
    const showing = input.type === "password";
    input.type = showing ? "text" : "password";
    b.setAttribute("aria-label", showing ? "Ocultar senha" : "Mostrar senha");
  };
const states = [
  "AC",
  "AL",
  "AP",
  "AM",
  "BA",
  "CE",
  "DF",
  "ES",
  "GO",
  "MA",
  "MT",
  "MS",
  "MG",
  "PA",
  "PB",
  "PR",
  "PE",
  "PI",
  "RJ",
  "RN",
  "RS",
  "RO",
  "RR",
  "SC",
  "SP",
  "SE",
  "TO",
];
options(
  $("addressform").elements.state,
  states.map((state) => ({ id: state, name: state })),
  "Selecione",
);
submit("search", async () => {
  page = 0;
  await catalog();
});
for (const id of ["categories", "stores"])
  $(id).onchange = () =>
    guard(async () => {
      page = 0;
      await catalog();
    });
$("previous").onclick = () =>
  guard(async () => {
    page = Math.max(0, page - 1);
    await catalog();
  });
$("next").onclick = () =>
  guard(async () => {
    page++;
    await catalog();
  });
$("ordersprevious").onclick = () =>
  guard(async () => {
    ordersPage = Math.max(0, ordersPage - 1);
    await orders();
  });
$("ordersnext").onclick = () =>
  guard(async () => {
    ordersPage++;
    await orders();
  });
submit("login", async (form) => {
  await token();
  const response = await fetch("/api/auth/login", {
    method: "POST",
    headers: {
      [csrf.headerName]: csrf.token,
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: new URLSearchParams(values(form)),
  });
  if (!response.ok)
    throw Error("E-mail ou senha incorretos. Confira seus dados.");
  form.reset();
  await token();
  management.reset();
  await identify();
  navigate(returnTo);
  returnTo = "catalog";
});
submit("register", async (form) => {
  await api("/auth/register", "POST", values(form));
  const email = form.elements.email.value;
  form.reset();
  authTab("login");
  $("login").elements.email.value = email;
  notify("Conta criada! Entre com seu e-mail e senha.");
  $("login").elements.password.focus();
});
submit("profileform", async (form) => {
  await api("/me", "PUT", values(form));
  await identify();
  notify("Seus dados foram atualizados.");
});
submit("passwordform", async (form) => {
  await api("/me/password", "PUT", values(form));
  form.reset();
  csrf = null;
  await token();
  management.reset();
  await identify();
  authTab("login");
  await show("account");
  notify("Senha alterada. Entre novamente com sua nova senha.");
});
submit("addressform", async (form) => {
  await api("/me/addresses", "POST", values(form));
  form.reset();
  checkoutKey = null;
  await addresses();
  notify("Endereço adicionado.");
});
$("checkout").onchange = () => {
  checkoutKey = null;
};
submit("checkout", async (form) => {
  const body = values(form);
  body.addressId = Number(body.addressId);
  checkoutKey ??= crypto.randomUUID();
  const order = await api("/me/checkout", "POST", body, {
    "Idempotency-Key": checkoutKey,
  });
  checkoutKey = null;
  ordersPage = 0;
  await updateCartCount();
  navigate("orders");
  notify(`Pedido #${order.id} criado! Acompanhe a confirmação do pagamento.`);
});
$("logout").onclick = () =>
  guard(async () => {
    closeDialogs();
    await api("/auth/logout", "POST");
    csrf = null;
    await token();
    management.reset();
    checkoutKey = null;
    await identify();
    navigate("catalog");
    notify("Você saiu da sua conta. Até logo!");
  });
window.addEventListener("hashchange", () =>
  guard(() => show(location.hash.slice(1) || "catalog")),
);
guard(async () => {
  await token();
  await identify();
  await refreshLookups();
  await show(location.hash.slice(1) || "catalog");
});
