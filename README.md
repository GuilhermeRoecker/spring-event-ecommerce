# E-commerce em Spring

Recriação do projeto E-commerce-Node.js em Java 21 e Spring Boot 3.5.11. A aplicação inclui catálogo, contas, empresas, categorias, carrinho, endereços, cupons, pedidos, registro de pagamento e acompanhamento de entrega. A interface está em português e é servida pela própria aplicação.

O código executável fica em `src/` e o `pom.xml` da raiz é a entrada do projeto. Os antigos esqueletos `order-service`, `inventory-service` e `payment-service` foram removidos porque não tinham regras de negócio e não participavam da aplicação. Esta implementação usa uma aplicação modular e uma transação de banco para checkout; não depende de Kafka.

As mudanças e diferenças em relação ao Node estão em [README-MIGRACAO.md](README-MIGRACAO.md). O contrato das rotas está em [docs/API.md](docs/API.md).

## Executar localmente

Requisitos: JDK 21 ou superior e Maven, ou o wrapper incluído. O primeiro build precisa de internet para baixar as dependências.

```bash
export BOOTSTRAP_ADMIN_EMAIL='admin@example.com'
export BOOTSTRAP_ADMIN_PASSWORD='uma-senha-forte-de-12-ou-mais-caracteres'
./mvnw spring-boot:run
```

Abra http://localhost:8080. O banco H2 é persistido em `data/`; o Flyway cria as tabelas. Sem as variáveis de bootstrap, a aplicação inicia, mas não cria administrador. Não há senha padrão. O bootstrap cria um administrador somente se ainda não existir nenhum; não promove uma conta existente nem redefine senhas.

Entre com o administrador e use **Gestão** para criar uma empresa e categoria e, depois, um produto. Os IDs das empresas e categorias podem ser consultados em `/api/companies` e `/api/categories`. Uma conta criada na tela de cadastro sempre recebe o cargo `CUSTOMER`. O administrador pode atribuir `OWNER` ou `SELLER` e uma empresa pelo painel. Para criar produtos como administrador, informe a empresa desejada; vendedores e donos só podem operar na própria empresa.

O fluxo de compra é: criar conta → entrar → adicionar endereço em Minha conta → adicionar produtos ao carrinho → criar pedido → consultar Pedidos. A confirmação manual do pagamento e a atualização da entrega ficam em Gestão para o administrador.

## Executar com Docker e PostgreSQL

```bash
cp .env.example .env
# Edite .env com suas próprias senhas e e-mail.
docker compose up --build
```

Acesse http://localhost:8080. O PostgreSQL fica na rede interna do Compose e persiste no volume `postgres_data`. `docker compose down` encerra os serviços e preserva os dados. Para usar PostgreSQL sem Compose, defina `DB_URL=jdbc:postgresql://localhost:5432/ecommerce`, `DB_USER` e `DB_PASSWORD` antes de executar o Maven.

Em hospedagem com HTTPS, use `COOKIE_SECURE=true`. O padrão `false` permite testar em HTTP local. A aplicação usa sessão no servidor, cookie HttpOnly/SameSite e CSRF. A interface obtém o token CSRF antes das mutações e o renova após login/logout.

## Interface Arôme

O visual retoma a identidade do projeto original: cabeçalho roxo, detalhes em rosa, fonte Lexend, menu lateral e login com abas sobre o fundo original. Há tema claro/escuro e layouts adaptados para celular.

- Início e filtros: `http://localhost:8080/#catalog`.
- Login/cadastro ou perfil da conta conectada: `http://localhost:8080/#account`.
- Carrinho e pedidos: `/#cart` e `/#orders`.
- Gestão: Menu → Gestão (`/#manage`), conforme o cargo e a empresa da conta.

A gestão usa tabelas e formulários para empresas, produtos, categorias, equipe, cupons e vendas. Os campos apresentam nomes de lojas e categorias em vez de exigir seus IDs. Os pedidos mostram endereço, itens e acompanhamento em português. Fontes, ícones e imagens de fundo são locais; imagens de produtos por URL continuam dependendo do endereço informado.

Para aplicar alterações visuais quando a aplicação está no Docker, execute `docker compose up --build --no-deps -d app` e atualize a página. Esse comando reconstrói somente a aplicação e preserva o banco; é necessário entrar novamente se havia uma sessão aberta.

## Dados fictícios para testes

Com o PostgreSQL do Docker Compose em execução:

```bash
docker compose exec -T db psql -U ecommerce -d ecommerce -v ON_ERROR_STOP=1 < scripts/seed-demo.sql
```

O script adiciona três lojas de demonstração, cinco categorias e quinze produtos. Inclui produto sem estoque, última unidade e produto inativo para testar esses cenários. Pode ser reexecutado sem duplicar os registros nem redefinir estoque, preços ou edições existentes. Não cria usuários ou pedidos e não é executado automaticamente ao iniciar a aplicação.

## Testes e build

```bash
./mvnw verify
java -jar target/ecommerce-1.0.0.jar
```

A suíte cobre autenticação, CSRF, exposição de campos, elevação de privilégios, isolamento por empresa e usuário, consultas parametrizadas, limites de paginação, validação, cupons, rollback, idempotência e concorrência no estoque. Para repetir os mesmos cenários em um **banco PostgreSQL exclusivo para testes**:

```bash
export TEST_POSTGRES_URL='jdbc:postgresql://localhost:5432/ecommerce_test'
export TEST_POSTGRES_USER='ecommerce'
export TEST_POSTGRES_PASSWORD='sua-senha-de-teste'
./mvnw test -Dtest=PostgresIntegrationTest
```

Os testes apagam os dados das tabelas da aplicação no banco de teste a cada cenário. Sem `TEST_POSTGRES_URL`, a suíte PostgreSQL é ignorada. O workflow em `.github/workflows/verify.yml` executa H2 e PostgreSQL com Java 21.

## Organização

Há um único projeto Maven, com os pacotes organizados por funcionalidade e separados em controllers, services, repositories e DTOs. As rotas são declaradas nos controllers; regras e transações ficam nos services, e o SQL fica nos repositories. Não existem serviços independentes para iniciar. O detalhamento da estrutura e das responsabilidades está em [docs/ARQUITETURA.md](docs/ARQUITETURA.md).

- `auth`: autenticação, perfil, senha, cargos e bootstrap do administrador.
- `catalog`: produtos, imagens por URL HTTPS, categorias e empresas.
- `commerce`: endereços, carrinho, checkout, cupons, pedidos, pagamentos e entregas.
- `shared`: acesso parametrizado ao banco e tradução de erros HTTP.
- `src/main/resources/db/migration`: evolução versionada do esquema.
- `src/main/resources/static`: interface sem dependências de Node.

As consultas privadas usam o ID da conta autenticada; a API não recebe `pessoaId` ou `carrinhoId` para escolher de quem comprar. O preço vem do banco, os valores usam `BigDecimal`, e os dados do produto e endereço são copiados para o pedido para preservar seu histórico.

Pagamento é um registro administrativo, sem cobrança real nem armazenamento de cartão. Frete, integração com transportadora, envio de e-mail e importação do banco MySQL antigo não estão conectados. Veja as decisões e limites no documento da migração.
