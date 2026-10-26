# Organização do código

A aplicação é um monólito modular: um processo Spring Boot, um banco e um projeto Maven. Os pacotes separam funcionalidades dentro da mesma aplicação; não são microsserviços nem módulos Maven independentes.

```text
.
├── pom.xml                      # Build único da aplicação
├── mvnw / mvnw.cmd / .mvn/       # Maven Wrapper
├── Dockerfile                   # Imagem da aplicação
├── docker-compose.yml           # Aplicação e PostgreSQL
├── docs/                        # API e arquitetura
└── src/
    ├── main/
    │   ├── java/com/guilherme/ecommerce/
    │   │   ├── EcommerceApplication.java
    │   │   ├── auth/            # Contas, sessão e autorização
    │   │   ├── catalog/         # Empresas, categorias e produtos
    │   │   ├── commerce/        # Carrinho, compra, pagamento e entrega
    │   │   └── shared/          # Acesso ao banco e erros HTTP
    │   └── resources/
    │       ├── application.properties
    │       ├── db/migration/    # Migrações Flyway
    │       └── static/          # HTML, CSS e JavaScript
    └── test/java/com/guilherme/ecommerce/
        ├── EcommerceIntegrationTest.java
        └── PostgresIntegrationTest.java
```

## Responsabilidades

| Local | Responsabilidade |
| --- | --- |
| `auth/SecurityConfig` | Configurar login, logout, sessão, CSRF e acesso inicial às rotas |
| `auth/CurrentAccount` | Obter a identidade e validar cargo e empresa a partir do banco |
| `auth/AccountController` | Cadastro, perfil, troca de senha e administração de membros |
| `auth/AdminBootstrap` | Criar o primeiro administrador mediante configuração explícita |
| `catalog/CatalogController` | Consultar e administrar empresas, categorias e produtos; inclui estoque e imagem |
| `commerce/CommerceController` | Endereços, carrinho, entrada do checkout e consulta de pedidos |
| `commerce/CheckoutService` | Coordenar a transação de compra, bloqueios de estoque, cupom, pedido e idempotência |
| `commerce/OperationsController` | Administração de cupons, confirmação de pagamento, entrega e consulta de vendas |
| `shared/Database` | Executar consultas parametrizadas e tratar recurso ausente |
| `shared/Errors` | Traduzir validação e conflitos para respostas HTTP controladas |

O fluxo de compra passa por `CommerceController → CheckoutService → Database`. As consultas e operações de cadastro mais simples usam `Database` diretamente nos controllers. A aplicação ainda não separa todos os casos de uso em serviços ou repositórios por domínio; essa é uma possibilidade de refatoração, não uma camada já existente.

## Por que os diretórios antigos foram removidos

`inventory-service`, `order-service` e `payment-service` eram projetos iniciais do Spring, sem funcionalidades implementadas. Não eram versões corretas ou completas dos respectivos domínios. Mantê-los junto à aplicação funcional sugeria três pontos de entrada adicionais e dificultava identificar qual projeto executar.

As responsabilidades correspondentes estão no catálogo e no pacote `commerce`, conforme a tabela acima. Não é necessário mover o código para diretórios com sufixo `-service`: esses nomes sugeririam aplicações independentes que a arquitetura atual não possui.

## Transação e uma possível separação futura

O checkout precisa confirmar ou reverter, em conjunto, o pedido, a baixa de estoque, o uso do cupom e a limpeza do carrinho. A aplicação única permite fazer isso em uma transação local. O pagamento criado nessa etapa é apenas um registro pendente; sua confirmação é administrativa.

Uma separação futura em microsserviços exigiria contratos entre serviços, reserva e liberação de estoque, processamento idempotente de eventos, publicação confiável e compensações em caso de falha. Criar três diretórios ou três classes Spring não oferece essas garantias. Por isso, a estrutura atual mantém explícita a arquitetura que está implementada.
