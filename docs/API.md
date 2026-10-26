# Contrato HTTP

Base: `/api`. JSON em UTF-8, valores monetários em reais, IDs inteiros. Campos desconhecidos no JSON são rejeitados. Erros de validação usam HTTP 400, ausência de sessão 401, acesso negado 403, recurso privado inexistente/inacessível 404 e conflito 409. Um ID de outra conta não dá acesso ao recurso. As listas privadas são limitadas à conta autenticada, inclusive para administradores; a gestão tem rotas próprias.

## Autenticação e CSRF

1. `GET /auth/csrf`: retorna `{ "token": "...", "headerName": "X-CSRF-TOKEN" }` e inicia a sessão. Guarde o cookie `JSESSIONID`.
2. Em POST, PUT e DELETE, envie o token no header indicado e o cookie da sessão.
3. `POST /auth/login`: **application/x-www-form-urlencoded**, campos `email` e `password`. Retorna 204 ou 401. A sessão é renovada no login; guarde o novo cookie e obtenha outro token CSRF.
4. `POST /auth/logout`: invalida a sessão, retorna 204. Obtenha outro token antes de novo cadastro/login.

Cadastro: `POST /auth/register`, JSON `{ "name": "Pessoa", "email": "pessoa@example.com", "password": "uma-senha-com-12-caracteres" }`. A senha deve ter 12 a 72 caracteres. O e-mail é normalizado para minúsculas. O cargo é sempre `CUSTOMER`. Retorna 201; duplicidade retorna 409.

A API usa a mesma origem da interface. Não é necessário CORS para executar o projeto. Não use o endpoint de login como JSON.

## Contas

| Método e caminho | Entrada | Regra |
| --- | --- | --- |
| GET `/me` | — | Dados públicos da própria conta |
| PUT `/me` | `{ "name": "Novo nome" }` | Edita apenas o próprio nome |
| PUT `/me/password` | `{ "currentPassword": "...", "newPassword": "..." }` | Confere senha atual e encerra sessão atual |
| GET `/admin/accounts?page=0` | — | ADMIN; 50 contas por página, sem hashes |
| PUT `/admin/accounts/{id}/membership` | `{ "role": "SELLER", "companyId": 1 }` | ADMIN; empresa obrigatória para SELLER/OWNER |

Cargos: `CUSTOMER`, `SELLER`, `OWNER`, `ADMIN`. As permissões são consultadas no banco em cada operação; roles enviadas pelo cliente não são fonte de autorização.

## Catálogo e empresas

| Método e caminho | Regra |
| --- | --- |
| GET `/products?page=0&size=20&search=livro&categoryId=1&companyId=1` | Público; filtros opcionais, página >=0, tamanho 1–100; só ativos |
| GET `/products/{id}` | Público; só produto ativo |
| GET `/manage/products?companyId=1` | SELLER/OWNER da empresa ou ADMIN; até 100, incluindo inativos |
| POST `/products` | SELLER/OWNER da empresa ou ADMIN; 201 |
| PUT `/products/{id}` | Mesma regra sobre a empresa do produto existente; substitui os campos editáveis |
| DELETE `/products/{id}` | Mesma regra; desativa sem apagar histórico |
| GET `/categories` | Público; até 500 |
| POST `/categories` | ADMIN; `{ "name": "Livros" }`; 201 |
| PUT `/categories/{id}` | ADMIN; `{ "name": "Novo nome" }` |
| DELETE `/categories/{id}` | ADMIN; referência por produto resulta em 409 |
| GET `/companies` | Público; até 500; `id`, `name`, `legal_name` |
| POST `/companies` | ADMIN; `{ "name": "Loja", "legalName": "Loja Ltda" }`; 201 |
| PUT `/companies/{id}` | OWNER da empresa ou ADMIN; mesmo JSON |

Produto (POST/PUT):

```json
{
  "companyId": 1,
  "categoryId": 1,
  "name": "Livro",
  "description": "Descrição do produto",
  "price": 39.90,
  "stock": 10,
  "active": true,
  "imageUrl": "https://example.com/livro.jpg"
}
```

`imageUrl` é opcional e deve usar HTTPS. Preço positivo com até duas casas decimais; estoque entre 0 e 1.000.000. Não é permitido transferir produto para outra empresa usando PUT.

As respostas SQL usam nomes em `snake_case` (`company_id`, `image_url`, `created_at` etc.); os DTOs de entrada usam os nomes `camelCase` dos exemplos. Essa convenção também é adotada pela interface.

## Endereços e carrinho

| Método e caminho | Comportamento |
| --- | --- |
| GET `/me/addresses` | Até 100 endereços próprios |
| POST `/me/addresses` | Cria endereço próprio; 201 |
| PUT `/me/addresses/{id}` | Substitui endereço próprio |
| DELETE `/me/addresses/{id}` | Exclui endereço próprio, sem alterar cópias históricas nos pedidos |
| GET `/me/cart` | Produtos, preços atuais e quantidades do próprio carrinho |
| PUT `/me/cart/{productId}` | `{ "quantity": 2 }`; quantidade absoluta, 1–999, validada contra estoque atual |
| DELETE `/me/cart/{productId}` | Remove item próprio; operação repetida é aceita |

Endereço:

```json
{
  "street": "Rua das Flores",
  "number": "10",
  "complement": "Apto 2",
  "city": "Orleans",
  "state": "SC",
  "postalCode": "88870000"
}
```

O carrinho não reserva estoque; a disponibilidade é validada novamente no checkout. Não há parâmetro para escolher o proprietário.

## Checkout e consulta de pedidos

`POST /me/checkout` exige o header `Idempotency-Key` com 8–80 caracteres de `a-z`, `A-Z`, `0-9`, `_` ou `-`:

```json
{ "addressId": 1, "method": "PIX", "coupon": "BEMVINDO" }
```

`method`: `PIX`, `CARD` ou `BOLETO`. `coupon` é opcional. Nunca envie número de cartão ou total. O pedido é criado com o preço atual dos produtos, desconto calculado no servidor e uma cópia do endereço. Retorno 201: `{ "id": 1, "total": 39.90, "status": "AWAITING_PAYMENT" }`.

Uma chave repetida pela mesma conta retorna o mesmo pedido sem repetir baixa de estoque ou consumo de cupom. A chave não deve ser reaproveitada para outra compra. Se o carrinho estiver vazio ou o cupom inválido, retorna 400; estoque insuficiente retorna 409. Qualquer falha reverte o checkout completo.

- `GET /me/orders?page=0`: 50 pedidos por página, do mais recente para o mais antigo.
- `GET /me/orders/{id}`: objeto com `order`, `items`, `payment` e `delivery`, apenas para o comprador.
- `GET /manage/order-items?companyId=1&page=0`: SELLER/OWNER da empresa ou ADMIN; 50 linhas de venda por página, sem expor o pedido completo de outras empresas nem dados pessoais do comprador.

## Cupons e operações administrativas

| Método e caminho | Entrada/comportamento |
| --- | --- |
| GET `/admin/coupons` | Até 500 cupons |
| POST `/admin/coupons` | `{ "code": "BEMVINDO", "percentage": 10, "expiresOn": "2099-12-31", "usageLimit": 100 }`; 201 |
| DELETE `/admin/coupons/{id}` | Desativa cupom; não apaga histórico |
| POST `/admin/orders/{id}/confirm-payment` | Sem corpo; `AWAITING_PAYMENT → PAID`; pagamento `PENDING → PAID` |
| PUT `/admin/orders/{id}/delivery` | `{ "status": "SHIPPED", "carrier": "Correios", "trackingCode": "BR123" }` |

Todas essas operações exigem ADMIN. O cupom vale até a data de expiração inclusive, usa código exato em maiúsculas, desconto de 1–100%, limite global e um uso por conta. O desconto é arredondado a centavos com `HALF_UP`.

A entrega aceita somente `PAID → SHIPPED → DELIVERED`. Não é permitido pular a confirmação de pagamento ou voltar o estado. A confirmação é administrativa e não executa cobrança em provedor externo.
