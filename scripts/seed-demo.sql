-- Dados fictícios para desenvolvimento local em PostgreSQL.
-- Executar na raiz: docker compose exec -T db psql -U ecommerce -d ecommerce -v ON_ERROR_STOP=1 < scripts/seed-demo.sql
-- Reexecutar não duplica registros nem redefine preços, estoque ou edições existentes.
BEGIN;
SELECT pg_advisory_xact_lock(482701263);

INSERT INTO company (name, legal_name)
SELECT seed.name, seed.legal_name
FROM (VALUES
    ('Tech Aurora (Demo)', 'Tech Aurora Demonstracao Ltda'),
    ('Casa Horizonte (Demo)', 'Casa Horizonte Demonstracao Ltda'),
    ('Livraria Caminhos (Demo)', 'Livraria Caminhos Demonstracao Ltda')
) AS seed(name, legal_name)
WHERE NOT EXISTS (SELECT 1 FROM company c WHERE c.legal_name = seed.legal_name);

INSERT INTO category (name)
VALUES ('Eletrônicos'), ('Acessórios'), ('Casa e decoração'), ('Livros'), ('Papelaria')
ON CONFLICT (name) DO NOTHING;

WITH seed(company_name, category_name, name, description, price, stock, active) AS (
    VALUES
    ('Tech Aurora (Demo)', 'Eletrônicos', 'Teclado mecânico compacto (Demo)', 'Teclado fictício com conexão USB e layout ABNT2. Produto de demonstração.', 249.90, 12, true),
    ('Tech Aurora (Demo)', 'Eletrônicos', 'Mouse sem fio ergonômico (Demo)', 'Mouse fictício com ajuste de sensibilidade e receptor USB.', 89.90, 25, true),
    ('Tech Aurora (Demo)', 'Acessórios', 'Suporte para notebook (Demo)', 'Suporte fictício ajustável para organização da mesa.', 119.50, 8, true),
    ('Tech Aurora (Demo)', 'Eletrônicos', 'Fone Bluetooth última unidade (Demo)', 'Estoque de uma unidade para testar duas compras concorrentes.', 179.90, 1, true),
    ('Tech Aurora (Demo)', 'Acessórios', 'Hub USB-C sem estoque (Demo)', 'Produto ativo com estoque zero para testar indisponibilidade.', 149.90, 0, true),
    ('Casa Horizonte (Demo)', 'Casa e decoração', 'Luminária de mesa (Demo)', 'Luminária fictícia de leitura com acabamento branco.', 129.90, 10, true),
    ('Casa Horizonte (Demo)', 'Casa e decoração', 'Kit de canecas cerâmicas (Demo)', 'Conjunto fictício com duas canecas para uso doméstico.', 59.90, 18, true),
    ('Casa Horizonte (Demo)', 'Casa e decoração', 'Organizador de bancada (Demo)', 'Organizador fictício com três compartimentos.', 39.90, 30, true),
    ('Casa Horizonte (Demo)', 'Casa e decoração', 'Almofada decorativa (Demo)', 'Almofada fictícia de tecido com capa removível.', 49.50, 15, true),
    ('Casa Horizonte (Demo)', 'Casa e decoração', 'Vaso fora de catálogo (Demo)', 'Produto inativo: deve aparecer na gestão, mas não no catálogo público.', 69.90, 5, false),
    ('Livraria Caminhos (Demo)', 'Livros', 'Introdução ao Java (Demo)', 'Livro fictício para testar o catálogo e o fluxo de compra.', 79.90, 20, true),
    ('Livraria Caminhos (Demo)', 'Livros', 'Spring na prática (Demo)', 'Livro fictício sobre desenvolvimento de aplicações e APIs.', 109.90, 14, true),
    ('Livraria Caminhos (Demo)', 'Livros', 'Fundamentos de SQL (Demo)', 'Livro fictício sobre consultas e modelagem relacional.', 64.90, 16, true),
    ('Livraria Caminhos (Demo)', 'Papelaria', 'Caderno pontilhado (Demo)', 'Caderno fictício para anotações e planejamento.', 29.90, 40, true),
    ('Livraria Caminhos (Demo)', 'Papelaria', 'Kit de marcadores de página (Demo)', 'Kit fictício de baixo valor para testar totais com centavos.', 9.99, 60, true)
)
INSERT INTO product (company_id, category_id, name, description, price, stock, active)
SELECT c.id, cat.id, seed.name, seed.description, seed.price, seed.stock, seed.active
FROM seed
JOIN company c ON c.name = seed.company_name AND c.legal_name IN (
    'Tech Aurora Demonstracao Ltda', 'Casa Horizonte Demonstracao Ltda', 'Livraria Caminhos Demonstracao Ltda'
)
JOIN category cat ON cat.name = seed.category_name
WHERE NOT EXISTS (SELECT 1 FROM product p WHERE p.company_id = c.id AND p.name = seed.name);

COMMIT;

SELECT c.id AS company_id, c.name AS company, count(p.id) AS products,
       count(p.id) FILTER (WHERE p.active) AS active_products
FROM company c
LEFT JOIN product p ON p.company_id = c.id
WHERE c.legal_name IN ('Tech Aurora Demonstracao Ltda', 'Casa Horizonte Demonstracao Ltda', 'Livraria Caminhos Demonstracao Ltda')
GROUP BY c.id, c.name
ORDER BY c.id;
