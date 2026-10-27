// All SVG markup is constant. Data received from the API is rendered with textContent.
const paths = {
  sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2m0 16v2M2 12h2m16 0h2M5 5l1.5 1.5m11 11L19 19M5 19l1.5-1.5m11-11L19 5"/>',
  moon: '<path d="M21 13a9 9 0 0 1-10-10 9 9 0 1 0 10 10Z"/>',
  user: '<circle cx="12" cy="8" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2"/>',
  cart: '<path d="M2 3h3l3 13h11l3-9H6M9 21h.01M18 21h.01"/><circle cx="9" cy="20" r="1"/><circle cx="18" cy="20" r="1"/>',
  menu: '<path d="M4 6h16M4 12h16M4 18h16"/>',
  search: '<circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/>',
  arrow: '<path d="M4 12h16m-6-6 6 6-6 6"/>',
  close: '<path d="m6 6 12 12M6 18 18 6"/>',
  grid: '<rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/>',
  store:
    '<path d="M3 9 5 3h14l2 6M3 9v3a3 3 0 0 0 6 0 3 3 0 0 0 6 0 3 3 0 0 0 6 0V9ZM5 15v6h14v-6M10 21v-5h4v5"/>',
  box: '<path d="m12 3 9 5-9 5-9-5 9-5Zm-9 5v10l9 5 9-5V8M12 13v10M7.5 5.5l9 5"/>',
  lock: '<rect x="4" y="10" width="16" height="12" rx="2"/><path d="M8 10V6a4 4 0 0 1 8 0v4M12 15v3"/>',
  eye: '<path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/>',
  logout: '<path d="M9 3H4v18h5M10 12h12m-5-5 5 5-5 5"/>',
  plus: '<path d="M12 4v16M4 12h16"/>',
  edit: '<path d="m15 5 4 4M4 20l4-1L21 6a3 3 0 0 0-4-4L4 15l-1 6ZM12 21h9"/>',
  pin: '<path d="M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 1 1 16 0Z"/><circle cx="12" cy="10" r="3"/>',
  tag: '<path d="M3 3h8l10 10-8 8L3 11V3Z"/><circle cx="7.5" cy="7.5" r="1"/>',
  book: '<path d="M12 5c-3-3-7-3-10-1v16c3-2 7-2 10 1 3-3 7-3 10-1V4c-3-2-7-2-10 1Zm0 0v16"/>',
  monitor:
    '<rect x="2" y="3" width="20" height="14" rx="2"/><path d="M12 17v5M7 22h10"/>',
  home: '<path d="m2 10 10-8 10 8M5 8v13h14V8M9 21v-8h6v8"/>',
  refresh:
    '<path d="M20 7V2m0 5h-5M4 17v5m0-5h5M20 7a9 9 0 0 0-16 2m0 8a9 9 0 0 0 16-2"/>',
};
export const $ = (id) => document.getElementById(id);
export const money = (v) =>
  new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(
    Number(v),
  );
export const date = (v) =>
  v
    ? new Intl.DateTimeFormat("pt-BR", { dateStyle: "medium" }).format(
        new Date(v.length === 10 ? v + "T12:00:00" : v),
      )
    : "—";
export const roles = {
  CUSTOMER: "Cliente",
  SELLER: "Vendedor",
  OWNER: "Dono de loja",
  ADMIN: "Administrador",
};
export const statuses = {
  AWAITING_PAYMENT: "Aguardando pagamento",
  PENDING: "Pendente",
  PAID: "Pago",
  SHIPPED: "Enviado",
  DELIVERED: "Entregue",
};
export function node(tag, className, text) {
  const el = document.createElement(tag);
  if (className) el.className = className;
  if (text !== undefined) el.textContent = text;
  return el;
}
export function icon(name) {
  const el = document.createElementNS("http://www.w3.org/2000/svg", "svg");
  el.setAttribute("viewBox", "0 0 24 24");
  el.setAttribute("fill", "none");
  el.setAttribute("stroke", "currentColor");
  el.setAttribute("stroke-width", "1.7");
  el.setAttribute("stroke-linecap", "round");
  el.setAttribute("stroke-linejoin", "round");
  el.setAttribute("class", "icon");
  el.setAttribute("aria-hidden", "true");
  el.innerHTML = paths[name] || paths.box;
  return el;
}
export function hydrateIcons(root = document) {
  for (const el of root.querySelectorAll("[data-icon]")) {
    const svg = icon(el.dataset.icon);
    if (el.tagName === "BUTTON") el.replaceChildren(svg);
    else el.replaceWith(svg);
  }
}
export function action(
  label,
  fn,
  { className = "", symbol, disabled = false } = {},
) {
  const b = node("button", className);
  b.type = "button";
  if (symbol) b.append(icon(symbol));
  b.append(document.createTextNode(label));
  b.disabled = disabled;
  b.onclick = fn;
  return b;
}
export function badge(text, type = "") {
  return node("span", "badge " + type, text);
}
export function statusBadge(value) {
  return badge(
    statuses[value] || value,
    ["PAID", "DELIVERED"].includes(value)
      ? "success"
      : value === "SHIPPED"
        ? ""
        : "warning",
  );
}
export function empty(title, description, symbol = "box") {
  const el = node("div", "empty-state");
  el.append(icon(symbol), node("h2", "", title), node("p", "", description));
  return el;
}
export function image(product, className = "product-image", symbol = "box") {
  const el = node("div", className);
  const fallback = () => el.replaceChildren(icon(symbol));
  if (product.image_url) {
    const img = node("img");
    img.alt = product.name;
    img.src = product.image_url;
    img.referrerPolicy = "no-referrer";
    img.loading = "lazy";
    img.onerror = fallback;
    el.append(img);
  } else fallback();
  return el;
}
export function table(headers, rows) {
  const wrap = node("div", "table-wrap"),
    t = node("table"),
    head = node("thead"),
    tr = node("tr");
  for (const h of headers) tr.append(node("th", "", h));
  head.append(tr);
  const body = node("tbody");
  for (const row of rows) {
    const tr = node("tr");
    for (const value of row) {
      const td = node("td");
      if (value instanceof Node) td.append(value);
      else td.textContent = value ?? "—";
      tr.append(td);
    }
    body.append(tr);
  }
  t.append(head, body);
  wrap.append(t);
  return wrap;
}
export function entity(name, subtitle, symbol = "store") {
  const el = node("div", "entity-name"),
    mark = node("span", "entity-icon"),
    text = node("div");
  mark.append(icon(symbol));
  text.append(node("strong", "", name));
  if (subtitle) text.append(node("small", "muted", subtitle));
  el.append(mark, text);
  return el;
}
export function options(select, rows, placeholder) {
  select.replaceChildren();
  if (placeholder) {
    const opt = node("option", "", placeholder);
    opt.value = "";
    select.append(opt);
  }
  for (const r of rows) {
    const opt = node("option", "", r.name);
    opt.value = r.id;
    select.append(opt);
  }
}
