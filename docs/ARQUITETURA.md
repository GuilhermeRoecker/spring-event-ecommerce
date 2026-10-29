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
    │       └── static/
    │           ├── index.html   # Estrutura das telas Arôme
    │           ├── app.js       # Navegação, conta e compra
    │           ├── style.css    # Estilos e temas responsivos
    │           └── assets/
    │               ├── js/      # Gestão e componentes visuais
    │               ├── fonts/   # Lexend e sua licença
    │               └── images/  # Fundos do original e favicon
    └── test/java/com/guilherme/ecommerce/
        ├── EcommerceIntegrationTest.java
        └── PostgresIntegrationTest.java
```

## Responsabilidades

O fluxo de uma requisição é `Controller → Service → Repository → Database`.
Os pacotes continuam organizados por funcionalidade (`auth`, `catalog` e `commerce`),
com as camadas identificadas pelo sufixo das classes dentro de cada pacote.

| Camada | Responsabilidade |
| --- | --- |
| `*Controller` | Declarar rotas com `@RequestMapping`, validar entradas com `@Valid`, receber parâmetros e definir status HTTP |
| `*Service` | Executar casos de uso, aplicar autorização e regras de negócio e delimitar transações com `@Transactional` |
| `*Repository` | Encapsular SQL parametrizado, consultas, gravações e bloqueios de linhas |
| `*Dtos` | Definir os records de entrada, suas validações e enums, sem dependência dos controllers |
| `shared/Database` | Adaptar o `JdbcClient`, executar SQL parametrizado e tratar recurso ausente |
| `shared/Errors` | Traduzir validação e conflitos para respostas HTTP controladas |

`AccountService` cuida do cadastro, perfil, senha e membros; `CatalogService`, dos
produtos, categorias e empresas; `CommerceService`, dos endereços, carrinho e consulta
de pedidos; `OperationsService`, dos cupons, vendas, pagamentos e entregas.
`CheckoutService` coordena a compra atômica por meio de `CheckoutRepository`.
Os repositories têm métodos nomeados e parâmetros tipados para cada operação.

No Spring MVC, os próprios controllers declaram as rotas; o contrato completo está
em [API.md](API.md). `SecurityConfig` configura login, logout, CSRF e acesso inicial
às rotas. A invalidação da sessão após trocar a senha permanece no controller,
após o retorno da transação do service.

`CurrentAccount` resolve a identidade autenticada e verifica permissões de cargo e
empresa. Ele, `SecurityConfig` e `AdminBootstrap` consultam `AccountRepository`,
que concentra a persistência de contas.

As transações que antes estavam nos controllers estão nos services. O checkout
mantém o bloqueio da conta para serializar alterações de carrinho/endereço e
retentativas, os bloqueios dos produtos em ordem de ID e o bloqueio do cupom.
O SQL continua usando JDBC e retornando mapas; a refatoração preserva o esquema,
as URLs e os formatos JSON usados pela interface.

`LayerArchitectureTest` verifica que os controllers dependem de services, que as
transações não ficam nos controllers e que os services não recebem DTOs aninhados
em controllers nem injetam o acesso genérico ao banco.

## Por que os diretórios antigos foram removidos

`inventory-service`, `order-service` e `payment-service` eram projetos iniciais do Spring, sem funcionalidades implementadas. Não eram versões corretas ou completas dos respectivos domínios. Mantê-los junto à aplicação funcional sugeria três pontos de entrada adicionais e dificultava identificar qual projeto executar.

As responsabilidades correspondentes estão no catálogo e no pacote `commerce`, conforme a tabela acima. Não é necessário mover o código para diretórios com sufixo `-service`: esses nomes sugeririam aplicações independentes que a arquitetura atual não possui.

## Transação e uma possível separação futura

O checkout precisa confirmar ou reverter, em conjunto, o pedido, a baixa de estoque, o uso do cupom e a limpeza do carrinho. A aplicação única permite fazer isso em uma transação local. O pagamento criado nessa etapa é apenas um registro pendente; sua confirmação é administrativa.

Uma separação futura em microsserviços exigiria contratos entre serviços, reserva e liberação de estoque, processamento idempotente de eventos, publicação confiável e compensações em caso de falha. Criar três diretórios ou três classes Spring não oferece essas garantias. Por isso, a estrutura atual mantém explícita a arquitetura que está implementada.
