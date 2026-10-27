# Migração do Node/Express para Spring

## Fonte e decisões
Foram examinados o servidor Express, as rotas, os middlewares, os controllers, os modelos, os scripts do navegador, os jobs e a estrutura do dump SQL do repositorio `E-commerce-Node.js`.

## Falhas encontradas e correções

| Código antigo | Problema observado | Implementação nova |
| --- | --- | --- |
| `routes/finalizarCompra.js` | Sem middleware de autenticação; aceita pessoa, carrinho e endereço do corpo sem verificar propriedade | Checkout autenticado, conta obtida da sessão, carrinho consultado por conta e endereço consultado por ID **e** proprietário |
| `middleware/validaUserId.js` | Validação genérica interpreta `:id` como item de carrinho, independentemente do recurso; listagens e corpos podem passar sem checagem | Cada operação privada restringe sua própria consulta pelo proprietário do recurso |
| `routes/produto.js` e `produtoController.js` | PATCH só exige login; operações não verificam empresa do produto existente | Escrita exige cargo e empresa do recurso persistido; transferência de empresa é rejeitada |
| `controllers/pessoaController.js` | PATCH copia todos os campos do corpo e interpola as chaves como colunas | DTOs explícitos, rejeição de campos desconhecidos e SQL com identificadores fixos |
| Pessoas/controle de acesso | Um usuário autenticado pode alcançar operações administrativas e consultas amplas | Administração exige cargo atual consultado no banco; cadastro público sempre cria cliente |
| `SELECT * FROM pessoa` | Retorna campos internos, incluindo hash de senha | Respostas de conta selecionam explicitamente campos públicos; hashes ficam apenas na autenticação |
| Consultas de listagem | Sem limite ou paginação; filtros não garantem autorização | Limites e paginação no catálogo, contas, vendas e pedidos; filtros privados incluem conta/empresa |
| Checkout | Consulta estoque e depois subtrai sem bloqueio | Bloqueio transacional dos produtos em ordem de ID e restrição de estoque não negativo |
| Checkout | Reenvio pode criar outra compra; valores monetários em ponto flutuante | Chave idempotente por conta e cálculos decimais com arredondamento explícito |
| Checkout | Pedido e pagamento imediatamente marcados como pagos por um mock | Pedido `AWAITING_PAYMENT`, pagamento `PENDING`; confirmação explícita restrita ao administrador |
| Checkout | Recebe endereço, mas não o grava no pedido | Endereço da conta validado e copiado para o histórico |
| Jobs de rastreamento | Atualização simulada de rastreio | Transições administrativas explícitas `PAID → SHIPPED → DELIVERED` |
| Cookies/JWT | Cargo fica congelado no token e logout só remove cookie | Sessão invalidada no logout; cargo e empresa consultados do banco nas operações |
| Retorno de erros | Algumas respostas incluem erro interno do banco | Validação e conflitos têm respostas controladas sem SQL ou stack trace |

Os valores das consultas antigas frequentemente já usavam `?`; o problema não era simplesmente a ausência de parâmetros. A interpolação de **nomes de colunas** no PATCH de pessoa e a falta de escopo de autorização eram falhas distintas. A nova busca concatena apenas cláusulas SQL fixas e vincula os valores separadamente. O termo de pesquisa é tratado como texto, sem virar expressão SQL.

## Funcionalidades e mudanças de contrato

| Área original | Nova solução |
| --- | --- |
| Login e logout | Login por formulário HTTP, sessão e CSRF; sem JWT no JavaScript |
| Pessoas/perfil | Cadastro, consulta e edição do próprio nome, troca de senha e administração separada de cargos/empresa |
| Empresas, vendedores e donos | Cadastro administrativo de empresas, edição pelo dono e atribuição administrativa de membros |
| Produtos e categorias | Catálogo público, filtros, paginação, gestão autorizada e desativação lógica de produtos |
| Imagens | URL HTTPS validada no produto, em vez de upload arbitrário no servidor |
| Carrinho e itens | Um carrinho lógico por conta, quantidade absoluta por produto e exclusão de item |
| Endereços | CRUD do próprio usuário; cópia histórica no pedido |
| Cupons e usuário-cupom | Cupom percentual com validade, limite global e um uso por conta, aplicado atomicamente no checkout |
| Pedidos e itens | Criados exclusivamente pelo checkout; consulta somente do comprador; vendedor consulta seus próprios itens vendidos |
| Pagamentos e entregas | Criados pelo checkout; confirmação/transições administrativas, sem CRUD livre de status financeiros |
| Controle de acesso | Regras centralizadas por cargo, empresa e propriedade; sem endpoint público para editar autorização |
| Segurança da conta | BCrypt, troca de senha com confirmação da senha atual, sessão, CSRF e logout |
| Frontend | Interface nova servida pelo Spring: catálogo, contas, endereços, carrinho, pedidos e gestão |

As rotas antigas não são aliases para a nova API: o novo contrato usa `/api` e está em [docs/API.md](docs/API.md). O frontend legado não deve ser apontado diretamente para os endpoints novos.

## Observações e limites

- O cadastro foi simplificado para nome, e-mail e senha. Campos antigos de CPF/CNPJ, data de nascimento, telefone, tipo de pessoa e nome de usuário não foram migrados para o cadastro. Não há validação fiscal ou etária nesta versão.
- O cupom novo é percentual e utilizável uma vez por conta. A combinação antiga de desconto fixo/percentual e atribuições manuais de usuário-cupom não é preservada como CRUD.
- O envio genérico de e-mail e a verificação por código do protótipo não foram expostos na nova API. Não há recuperação de senha por e-mail nem confirmação de e-mail; não apresentar esse fluxo como disponível.
- Pagamento é confirmação manual do administrador. Não há gateway, webhook, cobrança, reembolso, expiração automática de pedidos nem liberação automática do estoque de pedidos não pagos. Um pedido criado mantém a baixa de estoque.
- Pedidos podem reunir produtos de várias empresas. Vendedores veem apenas as linhas da própria empresa, sem os dados pessoais do comprador; a administração coordena pagamento e entrega do pedido completo.
- As listagens auxiliares de empresas/categorias e de produtos na gestão têm limites fixos. O catálogo e os pedidos têm paginação; a interface também permite navegar pelas páginas de pedidos, contas e vendas.
- A mesma chave de idempotência devolve a primeira compra, mesmo que um corpo diferente seja reenviado. Para outra compra, use outra chave. A interface guarda a chave durante novas tentativas na mesma página; um recarregamento perde essa chave local.
- Sessões ficam na memória da instância e são perdidas ao reiniciar. Múltiplas réplicas exigem armazenamento compartilhado de sessões. A troca de senha encerra a sessão atual; não revoga outras sessões já abertas.
- A aplicação precisa de HTTPS e `COOKIE_SECURE=true` em hospedagem. Não há bloqueio distribuído de tentativas de login, MFA, auditoria persistente ou integração de identidade externa.
- H2 é a opção local. PostgreSQL é a opção no Compose. As migrações criam um esquema novo; o dump MySQL não é importado automaticamente. Uma importação real precisa mapear contas, cargos, empresas, relações e históricos, resolver duplicidades e preservar os hashes compatíveis após validação.
- O ZIP contém `.env` e dump. Trate as credenciais antigas como expostas e substitua-as antes de reutilizar serviços reais. O novo projeto usa variáveis de ambiente e não extrai segredos do ZIP para execução.

## Validação

A suíte automatizada executa os cenários HTTP com Spring Security e banco real, sem substituir os repositórios por mocks. Há um cenário com duas transações simultâneas tentando comprar a última unidade. A suíte PostgreSQL reaproveita os mesmos cenários da suíte H2. O CI também está configurado para Java 21 e PostgreSQL.

Validação local: 20 cenários passaram em H2 e os mesmos 20 passaram em PostgreSQL 17, incluindo concorrência e acesso aos recursos estáticos sem abertura das rotas privadas. Os 40 testes foram executados com sucesso em Java 21 dentro do Docker, e a imagem da aplicação compilou com esse JDK. Uma verificação HTTP real validou bootstrap do administrador, cookies de sessão, CSRF, cadastro, catálogo, endereço, carrinho, checkout, confirmação de pagamento e logout. No Chromium headless, o fluxo visual de cadastro, login, endereço, carrinho, compra, detalhes e troca de senha também passou, sem erros JavaScript. Para reproduzir as suítes, use as instruções do README. A interface JavaScript também pode ser verificada com `node --check --input-type=module < src/main/resources/static/app.js` (Node não é dependência de execução da aplicação).

## Recuperação do visual original

A interface foi adaptada após uma nova leitura do HTML e CSS do ZIP original. A marca Arôme, a paleta roxa/rosa, a fonte Lexend, as abas do login, os fundos e o menu lateral serviram de referência. As duas imagens de fundo foram reaproveitadas; a fonte foi incluída localmente com sua licença SIL Open Font License em `static/assets/fonts/OFL-Lexend.txt`.

A gestão deixou de exibir um seletor genérico de operações com respostas JSON e passou a ter áreas de empresas, produtos, categorias, equipe, cupons e vendas. Cadastros e edições usam modais; lojas, categorias e cargos são selecionados por nome. Os pedidos também passaram a apresentar itens, pagamento e entrega em componentes próprios.

Os módulos do navegador são `app.js` (fluxos e sessão), `assets/js/management.js` (gestão) e `assets/js/ui.js` (componentes visuais e formatação). Conteúdo vindo da API é inserido como texto; o JavaScript antigo de autenticação/JWT não foi copiado. O backend continua validando todas as permissões, independentemente dos botões visíveis.

A atualização inclui tema persistido no navegador, navegação por fragmentos de URL, paginação de pedidos/contas/vendas, estados vazios, botões de envio protegidos contra cliques repetidos e retorno ao login quando a sessão termina. O formulário de cadastro conserva os campos suportados pela API atual; recuperar o visual não reintroduz campos fiscais ou verificação por e-mail ausentes no backend.

No Chromium foram verificados catálogo, busca vazia, tema persistido, login/cadastro, endereços, carrinho, compra, detalhes do pedido, criação/edição de empresa, categoria e produto, atribuição de vendedor, cupom e confirmação de pagamento. Também foram verificados o acesso do vendedor à própria loja e os layouts de 1366 px e 390 px, sem rolagem horizontal da página. Tabelas largas possuem sua própria rolagem horizontal.
