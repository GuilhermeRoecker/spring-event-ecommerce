# Diagnóstico e plano da refatoração

## Estado inicial (antes de alterações de implementação)

Inspecionados os 24 arquivos Java de produção, os três testes, pom, configuração,
migração V1, scripts SQL, documentação, CI, Docker e consumidores do frontend.
O repositório estava limpo. O sistema é um monólito Spring Boot 3.5.11/Java 21.

| Classes atuais | Responsabilidade e diagnóstico |
| --- | --- |
| AccountController / AccountService | Conta, cadastro, perfil, senha e membros; controller invalida sessão após commit |
| CurrentAccount | Identidade da sessão e autorização atual consultada no banco; preservar |
| SecurityConfig | BCrypt 12, UserDetailsService, sessão, login/logout e CSRF já presentes |
| AdminBootstrap | Administrador opcional por ambiente; não promover conta existente |
| CatalogController / CatalogService | Produtos, categorias e empresas; separar contratos e persistência |
| CommerceController / CommerceService | Endereços, carrinho, pedidos; responsabilidades demais e dependência em checkout apenas para lock |
| CheckoutService | Transação atômica de compra; mapas tornam campos, ordem e tipos implícitos |
| OperationsController / OperationsService | Cupons, vendas, pagamento e entrega; separar por caso de uso |
| Cinco repositories | SQL parametrizado e locks corretos, mas devolvem mapas e tipos JDBC |
| Database | Executor JDBC genérico, acopla ausência de linha a HTTP 404 |
| Errors | Advice já produz ProblemDetail para validação, status e conflitos |
| Quatro arquivos Dtos | Records válidos, porém contratos agrupados e enums de domínio misturados |

Os controllers já delegam e não acessam SQL. Não existe justificativa para criar
interfaces artificiais em todos os serviços. Há retornos Object e Map usados como
contrato, java.sql.Date no checkout, endereço composto pela ordem de um mapa e
ausência de um modelo explícito para estados de pedido. O frontend usa sessão e
token CSRF mascarado obtido em /api/auth/csrf; preservá-los evita uma segunda
autenticação. As permissões de ADMIN/OWNER/SELLER vêm do banco, não da role em sessão.

## Regras e contratos que devem permanecer

- Rotas /api, status 200/201/204 e respostas snake_case, exceto /me camelCase.
- Cadastro sempre CUSTOMER; BCrypt compatível, limite UTF-8 de 72 bytes.
- Conta/endereço/pedido restritos ao proprietário; gestão restrita à empresa/cargo.
- Catálogo somente ativo, exclusão lógica, preço positivo e quantidades limitadas.
- Carrinho não reserva estoque. Alterações e checkout serializados pela conta.
- Checkout bloqueia produtos por ID crescente e depois cupom; rejeita indisponíveis.
- Idempotência por conta/chave devolve primeiro pedido mesmo com corpo diferente.
- Cupom exato, válido inclusive na data final, um uso por conta e limite global.
- BigDecimal e HALF_UP em centavos; snapshots de preço/nome/empresa/endereço.
- Pedido AWAITING_PAYMENT e pagamento PENDING; confirmação manual apenas ADMIN.
- Transições AWAITING_PAYMENT → PAID → SHIPPED → DELIVERED; sem gateway.
- Vendas mostram apenas linhas da empresa sem dados pessoais do comprador.

## Arquitetura de destino

Backend no projeto Maven da raiz, frontend independente em frontend/. Features
account, catalog, commerce (endereços/carrinho), checkout, order e operations
(cupons e gestão). Camadas internas controller, dto/request, dto/response,
domain, service e repository somente quando usadas. Dependências de serviços
para repositories Spring Data JPA e autorização; controllers somente serviços.
Entidades nunca são contratos HTTP. Relações unidirecionais lazy; sem coleções
bidirecionais e cascatas destrutivas. Consultas projetadas e fetch explícito.

JPA/Hibernate é requisito explícito da seção 16 e substitui a recomendação
condicional de manter JDBC. Benefícios: modelos tipados, dirty checking e
repositories declarativos; custos: atenção ao flush, cache de primeiro nível,
lazy loading e N+1. Usar ddl-auto=validate, open-in-view=false e Flyway como
único dono do schema. V1 não será editada. Locks pessimistas preservam a
concorrência sem exigir colunas novas de versão. Não há necessidade de ManyToMany:
cart_item e coupon_redemption têm atributos próprios e são entidades de associação.

Angular standalone/strict, Material, Reactive Forms, rotas lazy, serviços HTTP,
guards funcionais e interceptor CSRF/sessão. Proxy local e Nginx na produção
mantêm mesma origem e dispensam CORS permissivo. Nada sensível em localStorage.
Remover o legado somente após validar a substituição.

## Sequência e verificações

1. Diagnóstico e linha de base: mvn test em H2; PostgreSQL quando disponível.
2. Separar DTOs/pacotes sem mudar contratos e executar suíte.
3. Entidades/repositories JPA e serviços tipados; preservar locks e transações.
4. Segurança e testes de regressão de autorização/CSRF/estados/idempotência.
5. Angular: catálogo/detalhe, autenticação, conta/endereços, carrinho/checkout,
   pedidos, empresas/produtos/categorias/membros/cupons/vendas/operações.
6. Build/testes Angular, Docker/Nginx/CI e testes E2E se executáveis no ambiente.
7. Revisar documentação e registrar resultados reais, limitações e pendências.

## Proteção existente

EcommerceIntegrationTest cobre sessão, CSRF, cadastro, validação, IDOR, cargo de
banco, empresa, catálogo/paginação/injeção, rollback, desconto, idempotência,
última unidade concorrente, estados e senha. PostgresIntegrationTest reutiliza
os cenários em banco exclusivo indicado por ambiente. LayerArchitectureTest
impede dependência controller → repository e SQL genérico em serviços.

## Execução

Primeira tentativa de linha de base impedida pela restrição de escrita no cache
Maven fora do workspace; execução com permissão solicitada. Resultados finais
serão registrados após os comandos, sem inferir sucesso de testes não executados.


## Implementação realizada

- Linha de base: 20 cenários H2 + 2 arquiteturais passaram; PostgreSQL ainda não
  estava configurado. A primeira separação de DTOs revelou erro no script de
  extração de records com expressões regulares de validação; foi corrigido antes
  da migração JPA.
- Concluída a migração das 12 tabelas para entidades e 12 JpaRepositories, removendo
  Database e todos os repositories JDBC de produção. Flyway V1 preservada.
- Módulos finais: account, catalog, commerce, coupon, order, checkout e shared.
  operations foi desmembrado em cupons e operações de pedido, evitando ciclos.
  Company pertence à identidade/associação empresarial de account; o catálogo
  a referencia. A documentação de arquitetura contém a árvore definitiva.
- DTOs de entrada separados; records de resposta explícitos preservam contratos.
  Checkout usa projeção escalar antes de adquirir locks; snapshots são tipados.
- Autorização continua consultando o banco. UserDetailsService separado;
  ProblemDetail para 401/403, sessão somente por cookies, logout remove JSESSIONID,
  e validação também nas entradas de services. CSRF continua habilitado.
- Angular 22.2.5/Material com rotas lazy, standalone, Reactive Forms, estado em
  Signals e serviços HTTP por funcionalidade. Templates extraídos e formatados.
  Reimplementados catálogo/detalhes, conta/endereço, carrinho/checkout, pedidos e
  todas as operações de gestão. Fontes/licença e identidade Arôme preservadas.
- Build Angular e 11 testes unitários passaram; E2E em Chromium contra Docker
  validou compra, gestão, permissões e entrega. Somente então o frontend estático
  foi removido. CI, Nginx, perfil de produção e Compose foram atualizados.
- A suíte passou inicialmente com 28 cenários em H2 e 28 em PostgreSQL 17, mais 4
  arquiteturais. Acrescentadas verificações de N+1 e disputa do último uso de cupom
  para a revisão final. Os resultados finais estão em REFACTOR_REPORT.md.

## Decisões preservadas

Sem JWT, gateways, ORM gerando schema, novas dependências de mapeamento, interfaces
artificiais, cascatas de exclusão de histórico ou microsserviços. O backend
permanece no projeto Maven da raiz e o Angular tem build/diretório próprio.
Nenhum commit foi criado automaticamente; as alterações permanecem revisáveis
no working tree. Não foram descartadas alterações anteriores do usuário.

## Encerramento da verificação

Build limpo Java 21 aprovado: 30 cenários H2, 30 PostgreSQL 17 e 4 arquiteturais,
sem testes ignorados. Angular: 11 unitários e build de produção aprovados.
Dois E2E passaram contra a imagem final sem legado, incluindo CSP e atributos
de cookies. Relatório completo, limitações e próximos passos em
[REFACTOR_REPORT.md](REFACTOR_REPORT.md).
