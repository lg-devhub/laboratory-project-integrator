-- Dados ficticios, exclusivos do ambiente local.
-- pgcrypto e usado para gerar e verificar os hashes BCrypt destes exemplos.
BEGIN;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO usuarios (nome, login, senha_hash, perfil, ativo) VALUES
    ('Administrador de teste', 'admin@stocka.local', crypt('Stocka123', gen_salt('bf', 10)), 'ADMINISTRADOR', TRUE),
    ('Operador de teste', 'operador@stocka.local', crypt('Stocka123', gen_salt('bf', 10)), 'USUARIO', TRUE),
    ('Usuario sem atribuicoes', 'semacesso@stocka.local', crypt('Stocka123', gen_salt('bf', 10)), 'USUARIO', TRUE),
    ('Usuario desativado', 'inativo@stocka.local', crypt('Stocka123', gen_salt('bf', 10)), 'USUARIO', FALSE)
ON CONFLICT DO NOTHING;

INSERT INTO ambientes (nome, descricao, tipo) VALUES
    ('Producao', 'Setor de producao usado nos testes locais.', 'SETOR'),
    ('Sala de manutencao', 'Ambiente de manutencao usado nos testes locais.', 'SALA')
ON CONFLICT DO NOTHING;

INSERT INTO usuario_ambiente (usuario_id, ambiente_id)
SELECT u.id, a.id
FROM usuarios u CROSS JOIN ambientes a
WHERE u.login IN ('operador@stocka.local', 'inativo@stocka.local')
  AND a.nome = 'Producao'
ON CONFLICT DO NOTHING;
COMMIT;
