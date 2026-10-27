import {
  $,
  node,
  icon,
  action,
  badge,
  statusBadge,
  entity,
  table,
  empty,
  options,
  money,
  date,
  roles,
} from "./ui.js";

export function createManagement({ api, getUser, notify, refreshLookups }) {
  let tab = "products",
    companyId = null,
    page = 0,
    version = 0;
  const content = $("managementcontent");
  const sections = {
    companies: ["Empresas", "store"],
    products: ["Produtos", "box"],
    categories: ["Categorias", "grid"],
    accounts: ["Equipe", "user"],
    coupons: ["Cupons", "tag"],
    sales: ["Vendas", "cart"],
  };
  const admin = () => getUser()?.role === "ADMIN";
  const available = () =>
    admin()
      ? Object.keys(sections)
      : getUser()?.role === "OWNER"
        ? ["companies", "products", "sales"]
        : ["products", "sales"];
  const run = async (fn) => {
    try {
      await fn();
    } catch (e) {
      notify(e.message, true);
    }
  };
  function tools(...buttons) {
    const el = node("div", "row-actions");
    el.append(...buttons);
    return el;
  }
  function toolbar(title, description, ...controls) {
    const el = node("div", "management-toolbar"),
      heading = node("div"),
      buttons = node("div", "management-tools");
    heading.append(node("h2", "", title), node("p", "", description));
    buttons.append(...controls);
    el.append(heading, buttons);
    content.append(el);
  }
  function button(label, fn, symbol = "edit", className = "button-outline") {
    return action(label, () => run(fn), { symbol, className });
  }
  function pager(rows) {
    const el = node("div", "pagination");
    el.append(
      action(
        "← Anterior",
        () => {
          page--;
          run(load);
        },
        { className: "button-outline", disabled: page === 0 },
      ),
      node("span", "", `Página ${page + 1}`),
      action(
        "Próxima →",
        () => {
          page++;
          run(load);
        },
        { className: "button-outline", disabled: rows.length < 50 },
      ),
    );
    content.append(el);
  }
  async function shops() {
    const all = await api("/companies");
    return admin() ? all : all.filter((c) => c.id === getUser().companyId);
  }
  function storeFilter(companies) {
    const select = node("select");
    select.setAttribute("aria-label", "Loja da gestão");
    options(select, companies);
    if (!companies.some((c) => c.id === companyId))
      companyId = companies[0]?.id ?? null;
    select.value = companyId ?? "";
    select.onchange = () => {
      companyId = Number(select.value);
      page = 0;
      run(load);
    };
    return select;
  }
  function form(
    title,
    fields,
    onSave,
    { submit = "Salvar", description = "" } = {},
  ) {
    const dialog = $("editdialog"),
      form = $("editform");
    form.replaceChildren();
    $("edittitle").textContent = title;
    if (description) form.append(node("p", "form-note", description));
    for (const f of fields) {
      const label = node("label", f.wide ? "span-all" : "", f.label);
      let input;
      if (f.type === "select") {
        input = node("select");
        options(input, f.options, f.placeholder);
      } else input = node(f.type === "textarea" ? "textarea" : "input");
      if (input.tagName === "INPUT") input.type = f.type || "text";
      input.name = f.name;
      input.required = f.required !== false;
      if (f.type === "checkbox") {
        label.className = "checkbox-label";
        input.required = false;
        input.checked = f.value ?? true;
      } else if (f.value !== undefined && f.value !== null)
        input.value = f.value;
      for (const attr of [
        "min",
        "max",
        "step",
        "maxLength",
        "placeholder",
        "pattern",
      ])
        if (f[attr] !== undefined && f.type !== "select") input[attr] = f[attr];
      if (f.disabled) input.disabled = true;
      label.append(input);
      if (f.help) label.append(node("small", "", f.help));
      form.append(label);
    }
    const feedback = node("p", "form-feedback span-all");
    feedback.id = "formfeedback";
    feedback.setAttribute("role", "alert");
    feedback.hidden = true;
    const actions = node("div", "dialog-actions"),
      save = node("button", "", submit);
    save.type = "submit";
    actions.append(
      action("Cancelar", () => dialog.close(), { className: "button-outline" }),
      save,
    );
    form.append(feedback, actions);
    form.onsubmit = async (e) => {
      e.preventDefault();
      save.disabled = true;
      feedback.hidden = true;
      const values = Object.fromEntries(new FormData(form));
      for (const f of fields) {
        if (f.type === "number")
          values[f.name] =
            values[f.name] === "" ? null : Number(values[f.name]);
        if (f.type === "checkbox")
          values[f.name] = form.elements[f.name].checked;
      }
      try {
        await onSave(values);
        dialog.close();
        notify("Alterações salvas com sucesso.");
        await load();
      } catch (error) {
        feedback.textContent = error.message;
        feedback.hidden = false;
      } finally {
        save.disabled = false;
      }
    };
    dialog.showModal();
  }
  function confirm(title, description, onSave) {
    form(title, [], onSave, { submit: "Confirmar", description });
  }
  function companyForm(c) {
    form(
      c ? "Editar empresa" : "Nova empresa",
      [
        {
          name: "name",
          label: "Nome fantasia",
          value: c?.name,
          maxLength: 160,
          wide: true,
        },
        {
          name: "legalName",
          label: "Razão social",
          value: c?.legal_name,
          maxLength: 200,
          wide: true,
        },
      ],
      async (values) => {
        await api(
          c ? "/companies/" + c.id : "/companies",
          c ? "PUT" : "POST",
          values,
        );
        await refreshLookups();
      },
    );
  }
  async function productForm(product, companies) {
    const categories = await api("/categories");
    if (!categories.length) {
      notify("Cadastre uma categoria antes de adicionar produtos.", true);
      return;
    }
    const selected = product?.company_id ?? companyId ?? companies[0]?.id;
    form(
      product ? "Editar produto" : "Novo produto",
      [
        {
          name: "name",
          label: "Nome do produto",
          value: product?.name,
          maxLength: 160,
          wide: true,
        },
        {
          name: "companyId",
          label: "Loja",
          type: "select",
          options: companies,
          value: selected,
          disabled: !!product || !admin(),
        },
        {
          name: "categoryId",
          label: "Categoria",
          type: "select",
          options: categories,
          value: product?.category_id,
        },
        {
          name: "description",
          label: "Descrição",
          type: "textarea",
          value: product?.description ?? "",
          maxLength: 2000,
          required: false,
        },
        {
          name: "price",
          label: "Preço (R$)",
          type: "number",
          min: "0.01",
          max: "999999999999.99",
          step: "0.01",
          value: product?.price,
        },
        {
          name: "stock",
          label: "Estoque",
          type: "number",
          min: 0,
          max: 1000000,
          step: 1,
          value: product?.stock ?? 0,
        },
        {
          name: "imageUrl",
          label: "Link da imagem",
          type: "url",
          value: product?.image_url ?? "",
          maxLength: 1000,
          required: false,
          wide: true,
          help: "Use um endereço de imagem que comece com https://.",
        },
        {
          name: "active",
          label: "Disponível no catálogo",
          type: "checkbox",
          value: product?.active ?? true,
        },
      ],
      async (values) => {
        values.companyId = Number(
          product?.company_id ?? values.companyId ?? selected,
        );
        values.categoryId = Number(values.categoryId);
        await api(
          product ? "/products/" + product.id : "/products",
          product ? "PUT" : "POST",
          values,
        );
      },
    );
  }
  async function load() {
    const stamp = ++version;
    content.replaceChildren(node("p", "muted", "Carregando…"));
    const companies = await shops();
    if (stamp !== version) return;
    content.replaceChildren();
    if (tab === "companies") {
      toolbar(
        "Empresas",
        "As lojas que fazem parte da Arôme.",
        ...(admin()
          ? [button("Nova empresa", () => companyForm(), "plus", "")]
          : []),
      );
      content.append(
        companies.length
          ? table(
              ["Empresa", "Razão social", "Ações"],
              companies.map((c) => [
                entity(c.name, `Loja #${c.id}`),
                c.legal_name,
                tools(button("Editar", () => companyForm(c))),
              ]),
            )
          : empty(
              "Sua próxima loja começa aqui",
              "Cadastre uma empresa para organizar seus produtos.",
              "store",
            ),
      );
    }
    if (tab === "products") {
      const filter = storeFilter(companies);
      toolbar(
        "Produtos",
        "Organize seu catálogo, preços e estoque.",
        filter,
        button("Novo produto", () => productForm(null, companies), "plus", ""),
      );
      if (!companyId) {
        content.append(
          empty(
            "Nenhuma loja disponível",
            "Cadastre uma empresa antes de adicionar seus produtos.",
            "store",
          ),
        );
        return;
      }
      const rows = await api("/manage/products?companyId=" + companyId);
      if (stamp !== version) return;
      content.append(
        rows.length
          ? table(
              ["Produto", "Categoria", "Preço", "Estoque", "Status", "Ações"],
              rows.map((p) => [
                entity(p.name, `Produto #${p.id}`, "box"),
                node("span", "category-name", String(p.category_id)),
                money(p.price),
                badge(
                  p.stock + " un.",
                  p.stock === 0
                    ? "danger"
                    : p.stock === 1
                      ? "warning"
                      : "neutral",
                ),
                badge(
                  p.active ? "Ativo" : "Inativo",
                  p.active ? "success" : "neutral",
                ),
                tools(
                  button("Editar", () => productForm(p, companies)),
                  ...(p.active
                    ? [
                        button(
                          "Desativar",
                          () =>
                            confirm(
                              "Desativar produto?",
                              `${p.name} deixará de aparecer no catálogo. O histórico de pedidos será preservado.`,
                              () => api("/products/" + p.id, "DELETE"),
                            ),
                          "close",
                          "button-danger",
                        ),
                      ]
                    : []),
                ),
              ]),
            )
          : empty(
              "Nenhum produto por aqui",
              "Clique em Novo produto para começar seu catálogo.",
            ),
      );
      const categories = await api("/categories");
      if (stamp !== version) return;
      for (const cell of content.querySelectorAll(".category-name"))
        cell.textContent =
          categories.find((c) => String(c.id) === cell.textContent)?.name ??
          "—";
      content.append(
        node(
          "p",
          "table-note",
          `Mostrando até 100 produtos da loja selecionada, incluindo os inativos.`,
        ),
      );
    }
    if (tab === "categories") {
      const categoryForm = (c) =>
        form(
          c ? "Editar categoria" : "Nova categoria",
          [
            {
              name: "name",
              label: "Nome da categoria",
              value: c?.name,
              maxLength: 120,
              wide: true,
            },
          ],
          async (values) => {
            await api(
              c ? "/categories/" + c.id : "/categories",
              c ? "PUT" : "POST",
              values,
            );
            await refreshLookups();
          },
        );
      toolbar(
        "Categorias",
        "Cada produto no seu lugar.",
        button("Nova categoria", () => categoryForm(), "plus", ""),
      );
      const rows = await api("/categories");
      if (stamp !== version) return;
      content.append(
        rows.length
          ? table(
              ["Categoria", "Ações"],
              rows.map((c) => [
                entity(c.name, null, "grid"),
                tools(
                  button("Editar", () => categoryForm(c)),
                  button(
                    "Excluir",
                    () =>
                      confirm(
                        "Excluir categoria?",
                        `A categoria ${c.name} só pode ser removida se não estiver vinculada a produtos.`,
                        async () => {
                          await api("/categories/" + c.id, "DELETE");
                          await refreshLookups();
                        },
                      ),
                    "close",
                    "button-danger",
                  ),
                ),
              ]),
            )
          : empty(
              "Nenhuma categoria cadastrada",
              "Crie categorias para facilitar a busca de produtos.",
              "grid",
            ),
      );
    }
    if (tab === "accounts") {
      toolbar("Equipe e clientes", "Gerencie o acesso das contas cadastradas.");
      const rows = await api("/admin/accounts?page=" + page);
      if (stamp !== version) return;
      content.append(
        rows.length
          ? table(
              ["Pessoa", "Cargo", "Loja", "Ações"],
              rows.map((a) => [
                entity(a.name, a.email, "user"),
                badge(roles[a.role] || a.role),
                companies.find((c) => c.id === a.company_id)?.name ?? "—",
                tools(
                  button("Alterar acesso", () =>
                    form(
                      "Acesso de " + a.name,
                      [
                        {
                          name: "role",
                          label: "Cargo",
                          type: "select",
                          options: Object.entries(roles).map(([id, name]) => ({
                            id,
                            name,
                          })),
                          value: a.role,
                        },
                        {
                          name: "companyId",
                          label: "Loja",
                          type: "select",
                          placeholder: "Sem vínculo com loja",
                          options: companies,
                          value: a.company_id,
                          required: false,
                        },
                      ],
                      async (values) => {
                        values.companyId = values.companyId
                          ? Number(values.companyId)
                          : null;
                        await api(
                          "/admin/accounts/" + a.id + "/membership",
                          "PUT",
                          values,
                        );
                      },
                      {
                        description:
                          "Para vendedor e dono de loja, selecione a empresa correspondente.",
                      },
                    ),
                  ),
                ),
              ]),
            )
          : empty(
              "Nenhuma conta nesta página",
              "As pessoas cadastradas aparecem aqui.",
              "user",
            ),
      );
      pager(rows);
    }
    if (tab === "coupons") {
      toolbar(
        "Cupons",
        "Um incentivo a mais para o próximo achado.",
        button(
          "Novo cupom",
          () =>
            form(
              "Novo cupom",
              [
                {
                  name: "code",
                  label: "Código",
                  pattern: "[A-Z0-9_-]{3,40}",
                  maxLength: 40,
                  help: "Letras maiúsculas, números, hífen ou sublinhado.",
                },
                {
                  name: "percentage",
                  label: "Desconto (%)",
                  type: "number",
                  min: 1,
                  max: 100,
                  step: 1,
                },
                { name: "expiresOn", label: "Válido até", type: "date" },
                {
                  name: "usageLimit",
                  label: "Limite de usos",
                  type: "number",
                  min: 1,
                  step: 1,
                  value: 100,
                },
              ],
              (values) => api("/admin/coupons", "POST", values),
            ),
          "plus",
          "",
        ),
      );
      const rows = await api("/admin/coupons");
      if (stamp !== version) return;
      content.append(
        rows.length
          ? table(
              [
                "Cupom",
                "Desconto",
                "Validade",
                "Utilizações",
                "Status",
                "Ações",
              ],
              rows.map((c) => [
                entity(c.code, null, "tag"),
                c.percentage + "%",
                date(c.expires_on),
                `${c.used} de ${c.usage_limit}`,
                badge(
                  c.active ? "Ativo" : "Inativo",
                  c.active ? "success" : "neutral",
                ),
                tools(
                  ...(c.active
                    ? [
                        button(
                          "Desativar",
                          () =>
                            confirm(
                              "Desativar cupom?",
                              `O cupom ${c.code} deixará de aceitar novos usos.`,
                              () => api("/admin/coupons/" + c.id, "DELETE"),
                            ),
                          "close",
                          "button-danger",
                        ),
                      ]
                    : []),
                ),
              ]),
            )
          : empty(
              "Nenhum cupom cadastrado",
              "Crie seu primeiro cupom de desconto.",
              "tag",
            ),
      );
    }
    if (tab === "sales") {
      const filter = storeFilter(companies);
      toolbar(
        "Vendas",
        "Acompanhe os produtos vendidos pela sua loja.",
        filter,
        ...(admin()
          ? [button("Buscar pedido", () => orderActions(), "search")]
          : []),
      );
      if (!companyId) {
        content.append(
          empty(
            "Nenhuma loja disponível",
            "Selecione uma loja para consultar suas vendas.",
            "store",
          ),
        );
        return;
      }
      const rows = await api(
        "/manage/order-items?companyId=" + companyId + "&page=" + page,
      );
      if (stamp !== version) return;
      content.append(
        rows.length
          ? table(
              [
                "Pedido",
                "Produto",
                "Data",
                "Quantidade",
                "Valor",
                "Status",
                "Ações",
              ],
              rows.map((r) => [
                "#" + r.order_id,
                r.name,
                date(r.created_at),
                r.quantity,
                money(r.unit_price * r.quantity),
                statusBadge(r.status),
                tools(
                  ...(admin()
                    ? [
                        button("Gerenciar", () =>
                          orderActions(r.order_id, r.status),
                        ),
                      ]
                    : []),
                ),
              ]),
            )
          : empty(
              "As primeiras vendas vêm aí",
              "Os itens vendidos aparecerão aqui após a criação de um pedido.",
              "cart",
            ),
      );
      pager(rows);
    }
  }
  function orderActions(id, status) {
    const fields = [
      {
        name: "id",
        label: "Número do pedido",
        type: "number",
        min: 1,
        step: 1,
        value: id,
        wide: true,
      },
      {
        name: "operation",
        label: "Ação",
        type: "select",
        wide: true,
        options: [
          { id: "confirm", name: "Confirmar pagamento recebido" },
          { id: "SHIPPED", name: "Registrar envio" },
          { id: "DELIVERED", name: "Confirmar entrega" },
        ],
        value:
          status === "PAID"
            ? "SHIPPED"
            : status === "SHIPPED"
              ? "DELIVERED"
              : "confirm",
      },
      {
        name: "carrier",
        label: "Transportadora",
        required: false,
        maxLength: 120,
      },
      {
        name: "trackingCode",
        label: "Código de rastreamento",
        required: false,
        maxLength: 120,
      },
    ];
    form(
      "Gerenciar pedido",
      fields,
      async (values) => {
        if (values.operation === "confirm")
          await api("/admin/orders/" + values.id + "/confirm-payment", "POST");
        else {
          if (!values.carrier.trim() || !values.trackingCode.trim())
            throw Error(
              "Informe a transportadora e o rastreamento para atualizar a entrega.",
            );
          await api("/admin/orders/" + values.id + "/delivery", "PUT", {
            status: values.operation,
            carrier: values.carrier,
            trackingCode: values.trackingCode,
          });
        }
      },
      {
        description:
          "Confirme apenas pagamentos que você verificou. Para envio e entrega, informe os dados de rastreamento.",
      },
    );
  }
  async function open() {
    if (!getUser() || getUser().role === "CUSTOMER") {
      content.replaceChildren(
        empty(
          "Área de gestão",
          "Esta área está disponível para a equipe das lojas.",
          "lock",
        ),
      );
      $("managementtabs").replaceChildren();
      return;
    }
    if (!available().includes(tab)) tab = "products";
    $("managerrole").textContent = roles[getUser().role];
    const nav = $("managementtabs");
    nav.replaceChildren();
    for (const key of available()) {
      const b = button(
        sections[key][0],
        async () => {
          tab = key;
          page = 0;
          await open();
        },
        sections[key][1],
      );
      if (key === tab) b.setAttribute("aria-current", "page");
      nav.append(b);
    }
    await load();
  }
  return {
    open,
    reset() {
      companyId = null;
      page = 0;
      version++;
    },
  };
}
