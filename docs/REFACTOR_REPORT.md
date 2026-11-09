# Relatório da refatoração

## 1. Problemas arquiteturais encontrados

- DTOs agrupados, retornos Object e mapas cujas chaves/tipos dependiam do SQL.
- Serviços de comércio e operações reuniam endereços, carrinho, pedidos, cupons,
  vendas, pagamento e entrega. O carrinho dependia do checkout apenas para lock.
- Ausência de entidades/modelos explícitos de persistência e estados de pedido.
- Checkout acoplado a java.sql.Date e à ordem de valores de um mapa de endereço.
- UserDetailsService dentro de SecurityConfig; respostas 401/403 dos filtros
  divergiam das respostas ProblemDetail dos controllers.
- Frontend estático com manipulação manual do DOM, estado global, formulários e
  navegação sem contratos TypeScript ou suíte unitária.
- Testes arquiteturais limitados à lista fixa de classes, sem verificar ciclos,
  exposição de entidades ou regressão para JDBC.

O projeto já tinha controles importantes: controllers delegavam, consultas eram
parametrizadas, BCrypt e CSRF estavam ativos, permissões vinham do banco e o
checkout já usava transação, idempotência e locks ordenados. Essas garantias
foram preservadas; não se partiu da premissa de que era necessário substituir
sua autenticação ou seus comportamentos.

## 2. Nova árvore de pacotes

```text
com.guilherme.ecommerce
├── account
│   ├── controller
│   ├── dto/request, dto/response
│   ├── domain: Account, Company, Role
│   ├── repository
│   ├── security
│   └── service
├── catalog
│   ├── controller
│   ├── dto/request, dto/response
│   ├── domain: Product, Category
│   ├── repository
│   └── service
├── commerce
│   ├── controller: AddressController, CartController
│   ├── dto/request, dto/response
│   ├── domain: Address, CartItem, CartItemId
│   ├── repository
│   └── service: AddressService, CartService
├── coupon
│   ├── controller
│   ├── dto/request, dto/response
│   ├── domain: Coupon
│   ├── repository
│   └── service
├── order
│   ├── controller
│   ├── dto/request, dto/response
│   ├── domain: PurchaseOrder, OrderItem, Payment, Delivery e enums
│   ├── repository
│   └── service: OrderService, OrderOperationsService
├── checkout
│   ├── controller
│   ├── dto/request
│   ├── domain: CouponRedemption, CouponRedemptionId
│   ├── repository
│   └── service: CheckoutService
└── shared
    ├── Errors, Resources
    └── exception: BusinessException

frontend/src/app
├── core: auth, guards, interceptors, services
├── shared: components, models, validators
├── features: authentication, catalog, cart, checkout, orders, account, admin
└── layout
```

A árvore detalhada e o grafo de dependências estão em [ARQUITETURA.md](ARQUITETURA.md).
O Maven da raiz é exclusivamente backend; o Angular tem diretório, dependências,
Dockerfile e build próprios.

## 3. Principais decisões e justificativas

1. **JPA obrigatório, sem mudar os dados:** 12 entidades e 12 interfaces
   JpaRepository sobre o schema existente. Flyway V1 permaneceu intacta;
   nenhuma coluna/tabela adicional foi necessária. Hibernate valida o schema,
   não o gera. Implementações JDBC foram removidas da produção.
2. **Locks pessimistas preservados:** conta, produtos em ordem crescente, cupom
   e pedido mantêm serialização no banco. Não foi acrescentada uma coluna
   @Version desnecessária para essas operações. O checkout usa projeção escalar
   para evitar estoque carregado antes do lock pelo cache de primeiro nível.
3. **Contratos explícitos:** DTOs independentes e records de resposta; relações
   JPA nunca são serializadas. Campos snake_case e /me camelCase foram mantidos.
4. **Relações com propósito:** ManyToOne lazy, OneToOne/MapsId para pagamento e
   entrega, entidades de associação com identidade composta para carrinho e
   resgate. Sem coleções inversas, ManyToMany artificial ou exclusões em cascata.
5. **Serviços concretos e coesos:** interfaces de persistência e autenticação
   têm finalidade real; não se criou uma interface por service. AccountLockService
   exige transação existente e elimina a dependência do carrinho em checkout.
6. **Consultas e desempenho:** EntityGraph no carrinho/vendas, leitura por IDs
   nas referências e paginação preservada. Há teste com 20 produtos verificando
   limite de consultas, além dos testes de concorrência em PostgreSQL.
7. **Angular separado:** standalone, Router lazy, Reactive Forms, Signals,
   RxJS/HttpClient, serviços por funcionalidade e Material personalizado. Não
   foram mantidas duas interfaces concorrentes depois da validação E2E.
8. **Mesma origem:** proxy local e Nginx dispensam CORS permissivo, preservando
   cookies/sessão. Perfil de produção, runtime config público e Compose completo.

A migração foi executada diretamente no projeto. Não houve alteração de V1,
introdução de Lombok/MapStruct, gateway, JWT, mensageria ou microsserviços.

## 4. Melhorias de segurança implementadas

- UserDetailsService separado e tipado; BCrypt custo 12 e hashes compatíveis.
- Validação de DTOs também nas entradas de serviços; limite de senha em bytes
  UTF-8 continua protegendo o BCrypt.
- Autorizações por cargo/empresa consultadas no banco e propriedade nas queries.
  Teste demonstra revogação de ADMIN em uma sessão já autenticada.
- Erros de filtro 401/403 em ProblemDetail, sem SQL, hashes ou stack traces.
- Sessão somente por cookie; HttpOnly/SameSite=Lax mantidos, Secure por padrão
  em produção. Logout invalida sessão, limpa autenticação e remove JSESSIONID.
- CSRF permanece ativo em toda mutação, inclusive login/cadastro/logout.
  Interceptor renova a referência após mudanças de sessão e não repete mutações
  recusadas automaticamente. Sessão/token/senhas não vão para localStorage.
- Guards funcionais complementam a navegação; o backend continua autorizando.
- Nginx com CSP, proteção de enquadramento, nosniff e API da mesma origem.
  O E2E verificou CSP e cookies HttpOnly/SameSite no ambiente executado.
- Backend e PostgreSQL sem portas públicas no Compose; credenciais por ambiente.

## 5. Regras de negócio preservadas

Cadastro sempre CUSTOMER; membros alterados somente pelo administrador; vendedor
limitado à empresa e proprietário com permissão de edição de sua empresa.
Produtos ativos no catálogo, desativação lógica e proibição de transferência de
empresa. Endereços/pedidos privados por proprietário. Carrinho com quantidade
absoluta e sem reserva prévia de estoque.

Compra atômica com chave idempotente por conta, estoque validado sob lock,
cupom exato, validade inclusiva, limite global e um uso por conta. BigDecimal e
HALF_UP; snapshots de nome, preço, empresa e endereço. A mesma chave devolve o
primeiro pedido sem consumir novamente estoque/cupom, inclusive com retentativas
simultâneas. Falha após alterações intermediárias reverte a transação completa.

Pagamento PENDING e pedido AWAITING_PAYMENT; confirmação manual apenas ADMIN.
Transições PAID → SHIPPED → DELIVERED, sem cobrança real. Vendas da empresa não
expõem dados pessoais do comprador. Rotas REST, status de sucesso e formatos JSON
existentes foram preservados; a mudança de formato dos erros 401/403 está em API.md.

## 6. Testes executados e resultados

Verificações locais efetivamente concluídas nesta refatoração:

| Verificação | Ambiente | Resultado |
| --- | --- | --- |
| Linha de base anterior à migração | JVM local, H2 | 20 integração + 2 arquitetura passaram; PostgreSQL ignorado nessa primeira execução |
| Build final `clean verify` | Temurin Java 21.0.12.1 em Docker | BUILD SUCCESS; JAR gerado |
| EcommerceIntegrationTest final | H2 2.3.232, Java 21 | **30 passaram**, 0 falhas/erros |
| PostgresIntegrationTest final | PostgreSQL 17.11, Java 21 | **30 passaram**, 0 falhas/erros |
| LayerArchitectureTest final | Java 21 | **4 passaram**, 0 falhas/erros |
| `npm test` | Angular/Vitest, Node local | **11 passaram** em 4 arquivos |
| `npm run build` | Angular CLI, TypeScript estrito, produção | Passou, sem erro; rotas lazy geradas |
| Docker Compose isolado | Java 21 + PostgreSQL 17 + Angular/Nginx | Imagens construídas e stack iniciada; rebuild sem legado passou |
| `npm run e2e` final | Chromium real contra stack Docker final | **2 passaram**, sem erros JavaScript capturados |
| `git diff --check` | Working tree | Sem erros de whitespace |

**Total final: 77 testes passaram (64 backend + 11 Angular + 2 E2E).** O build
limpo final não ignorou testes PostgreSQL. A última revisão removeu imports
TypeScript não usados e ativou noUnusedLocals; testes e build Angular foram
repetidos após essa limpeza estática. A primeira execução usou Java 27 local;
a validação final do backend foi executada especificamente com Java 21.

Cenários adicionados: ProblemDetail de segurança, header obrigatório, revogação
em sessão aberta, autorização de dono/vendedor, snapshots e estados inválidos,
rollback após atualizações de estoque/cupom, duas retentativas da mesma chave,
cupom expirado/já usado/100%, último uso de cupom concorrente e proteção contra N+1.

O E2E fez navegação pública, busca sem resultados, redirecionamento privado,
layout de 390 px sem overflow horizontal e persistência de tema. O fluxo completo
criou empresa, categoria, produto e cupom pela gestão; cadastrou e autenticou
comprador; criou endereço; adicionou/alterou carrinho; comprou com cupom; consultou
pedido após reload; confirmou pagamento/envio/entrega; atribuiu vendedor e
verificou acesso a produtos/vendas e bloqueio da administração; desativou cupom;
trocou senha e autenticou com a nova senha. O screenshot móvel também foi
inspecionado. Todos os dados/credenciais dessa execução eram de um ambiente
isolado de validação, sem usar o banco normal da aplicação.

Durante a implementação houve falhas de compilação e uma expectativa E2E de
endereço sem seu complemento; foram corrigidas e as verificações correspondentes
reexecutadas. Não são apresentados como aprovados comandos que falharam. Os
avisos de SQL overflow nas suítes são provocados intencionalmente pelo cenário
que demonstra rollback integral, e não representam falha de teste.

## 7. Problemas e limitações conhecidos

- Sessões continuam na memória da instância; reiniciar encerra sessões. Trocar
  senha invalida a sessão atual, não todas as sessões do usuário.
- Não há auditoria persistente, MFA, recuperação de senha, confirmação de e-mail
  ou rate limiting distribuído. Essas capacidades não existiam e não foram
  anunciadas como implementadas.
- Sem cancelamento, expiração de pedidos, estorno ou devolução automática de
  estoque; pedido não pago mantém a baixa, conforme a regra anterior.
- Limites fixos de listas auxiliares, composição de endereço com campos opcionais
  e limite NUMERIC(14,2) permanecem. Overflow de total resulta em 409/rollback.
- Idempotency-Key fica em memória na tela de checkout; recarregar/navegar para
  outra compra exige revisar pedidos para evitar confundir uma resposta perdida
  com compra não realizada, como já ocorria antes.
- Não foi executado teste de carga, pentest, auditoria de acessibilidade completa
  ou matriz Firefox/WebKit. Responsividade verificada no Chromium não implica
  homologação universal.
- TLS/certificados de hospedagem e segredos reais dependem do ambiente. O Compose
  validado usa HTTP local. Não se afirma que HTTPS em produção foi testado.
- O CI foi atualizado, mas sua execução remota não foi acompanhada nesta sessão.
  Não houve implantação pública ou teste contra o banco de produção.
- Não foram criados commits automáticos; a alteração está disponível no working
  tree para revisão. Nenhum estado preexistente do usuário foi descartado.

Não ficaram pendentes a migração JDBC → JPA, a substituição Angular, a remoção do
legado ou as verificações locais descritas. As limitações acima delimitam o que
não se pode inferir desses resultados.

## 8. Melhorias futuras fora do escopo

- Armazenamento compartilhado de sessão e revogação global após troca de senha.
- Auditoria de mudanças administrativas, métricas de locks e rastreamento de
  requisições, sem registrar dados sensíveis.
- Rate limiting e proteção de credenciais com uma política operacional definida.
- Cancelamento/expiração de pedidos com reposição idempotente de estoque, se o
  negócio aprovar essa mudança explícita de comportamento.
- Recuperação de senha/e-mail, frete e gateway somente com requisitos próprios.
- Testes de carga/planos de consulta em volumes representativos e índices
  adicionais via novas versões Flyway, conforme evidência.
- E2E em Firefox/WebKit, auditoria de acessibilidade e mais cenários de gestão.
- Persistência controlada do identificador de uma tentativa de checkout para
  recuperação de resposta perdida após recarregamento, sem armazenar credenciais.

## Referências técnicas

Decisões conferidas com a documentação primária de
[compatibilidade Angular](https://angular.dev/reference/versions) e
[locking no Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html).
As versões concretas foram resolvidas em package-lock.json e verificadas pelos
builds; as evidências de funcionamento acima vêm das execuções locais.
