# Arôme — E-commerce full-stack

Monólito modular com **Java 21, Spring Boot 3.5.11, Spring Security, Spring Data JPA/Hibernate, PostgreSQL e Flyway**, com frontend separado em **Angular 22, TypeScript estrito e Angular Material**.

Inclui catálogo e filtros, detalhes de produtos, cadastro/login, perfil/senha, endereços, carrinho, checkout idempotente, cupons, pedidos, pagamento manual, entrega e gestão de empresas, produtos, categorias, membros e vendas. O frontend legado foi substituído após validação E2E; o Spring serve somente a API.

## Executar com Docker

```bash
cp .env.example .env
# Edite as senhas e o e-mail; não há credenciais padrão na aplicação.
docker compose up --build
```

Abra **http://localhost:8080**. O Nginx serve o Angular e encaminha `/api` ao backend. Apenas o frontend publica porta; banco e backend ficam na rede interna. `HTTP_PORT` altera a porta local. O volume `postgres_data` preserva os dados; `docker compose down` encerra os containers sem apagar o banco.

O bootstrap cria o primeiro administrador somente quando não existe ADMIN. Não promove uma conta existente nem redefine sua senha. Na gestão, crie empresa, categoria e produto. Cadastros públicos sempre criam CUSTOMER; a atribuição de SELLER/OWNER e empresa exige administrador.

Em produção, publique por HTTPS e configure `COOKIE_SECURE=true`. O perfil `prod` exige `DB_URL`, `DB_USER` e `DB_PASSWORD`, mantém `ddl-auto=validate` e tem cookies Secure por padrão. O Compose usa HTTP local e permite a configuração explícita de `COOKIE_SECURE=false`.

## Desenvolvimento

Backend: JDK **21** e Maven (ou wrapper). PostgreSQL é o banco principal; H2 é uma alternativa local e de teste.

```bash
export DB_URL='jdbc:postgresql://localhost:5432/ecommerce'
export DB_USER='ecommerce'
export DB_PASSWORD='sua-senha'
# Bootstrap opcional, sem conta administrativa automática quando omitido:
export BOOTSTRAP_ADMIN_EMAIL='admin@example.com'
export BOOTSTRAP_ADMIN_PASSWORD='sua-senha-forte'
./mvnw spring-boot:run
```

Sem `DB_URL`, o perfil padrão usa H2 em `data/`. O backend atende em `http://localhost:8080/api`; sua raiz não contém uma interface.

Frontend: Node **24.15+** (ou outra versão compatível com Angular 22), npm e Angular CLI instalado pelo projeto.

```bash
cd frontend
npm ci
npm start
```

Abra **http://localhost:4200**. O proxy em `frontend/proxy.conf.json` encaminha `/api` ao backend em 8080. Sessão, cookies e CSRF continuam na mesma origem; CORS não é necessário. A configuração pública em `frontend/public/config.js` define a base da API; no container, `API_URL` é aplicada na inicialização. Use um caminho da mesma origem e ajuste o proxy/Nginx se alterar `/api`.

```bash
# Build de produção do frontend:
npm run build
# Backend empacotado, executando a partir da raiz:
./mvnw package
java -jar target/ecommerce-1.0.0.jar
```

O build Angular produz `frontend/dist/storefront/browser`. Cada aplicação tem seu próprio Dockerfile e ciclo de build; o projeto Maven da raiz contém exclusivamente o backend. Não é necessário mover o Maven para outro diretório para separá-lo do Angular.

## Testes

```bash
./mvnw verify
cd frontend
npm ci
npm test
npm run build
```

Sem `TEST_POSTGRES_URL`, os testes PostgreSQL são ignorados. Para a suíte completa, use **um banco PostgreSQL exclusivo e descartável**, pois os testes limpam suas tabelas a cada cenário:

```bash
TEST_POSTGRES_URL='jdbc:postgresql://localhost:5432/ecommerce_test' \
TEST_POSTGRES_USER='ecommerce' TEST_POSTGRES_PASSWORD='senha-de-teste' \
./mvnw verify
```

Os cenários verificam contratos HTTP, BCrypt/UTF-8, CSRF/sessão, autorização atual no banco, IDOR, empresa, validação, estados, snapshots históricos, rollback, estoque/cupom concorrentes, idempotência simultânea e número de consultas no carrinho. Os testes arquiteturais verificam contratos tipados, ausência de JDBC na produção, isolamento das camadas e dependências acíclicas entre funcionalidades. JDBC permanece apenas nos fixtures/assertivas independentes de integração.

Para E2E, inicie **outro ambiente exclusivo de teste**, com um administrador e as credenciais correspondentes. Os testes criam contas, empresas, produtos, cupons e pedidos fictícios; não os aponte para produção.

```bash
cd frontend
npx playwright install chromium
export PLAYWRIGHT_BASE_URL='http://127.0.0.1:8080'
export E2E_ADMIN_EMAIL='seu-admin-de-teste@example.test'
export E2E_ADMIN_PASSWORD='senha-do-admin-de-teste'
npm run e2e
```

`PLAYWRIGHT_CHROMIUM_EXECUTABLE` permite usar um Chromium já instalado. O CI executa H2/PostgreSQL, testes/build Angular e E2E em Docker. A configuração de CI foi atualizada; sua execução remota não é presumida a partir dos testes locais.

## Variáveis

| Variável | Uso |
| --- | --- |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexão JDBC usada pelo datasource JPA/Flyway |
| `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD` | Bootstrap opcional do primeiro administrador |
| `SPRING_PROFILES_ACTIVE=prod` | PostgreSQL por ambiente e cookies Secure por padrão |
| `COOKIE_SECURE` | `true` com HTTPS; `false` somente em HTTP local |
| `HTTP_PORT` | Porta local do Nginx no Compose, padrão 8080 |
| `API_URL` | Base pública de API do container frontend, padrão `/api` |
| `TEST_POSTGRES_*` | Banco exclusivo da suíte de integração |
| `PLAYWRIGHT_BASE_URL`, `E2E_ADMIN_*` | Ambiente e administrador exclusivos de E2E |

## Documentação e limites

- [Arquitetura e limites transacionais](docs/ARQUITETURA.md).
- [Contratos REST](docs/API.md).
- [Diagnóstico, plano e execução da refatoração](docs/REFACTOR_PLAN.md).
- [Relatório final e evidências de validação](docs/REFACTOR_REPORT.md).
- [Histórico da migração Node → Spring](README-MIGRACAO.md).

O script opcional `scripts/seed-demo.sql` adiciona lojas/categorias/produtos sem criar usuários ou pedidos:

```bash
docker compose exec -T db psql -U ecommerce -d ecommerce -v ON_ERROR_STOP=1 < scripts/seed-demo.sql
```

Pagamento continua sendo confirmação administrativa, sem gateway ou dados de cartão. Não há cálculo de frete, recuperação por e-mail, expiração/cancelamento de pedidos, reembolso ou reposição automática do estoque de pedidos não pagos. Sessões são locais à instância e a troca de senha encerra a sessão atual; revogação global e operação com várias réplicas exigem trabalho adicional.
