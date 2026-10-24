-- STOCKA: primeiro incremento do banco.
-- Este arquivo cria a estrutura; nao implementa a API nem a autorizacao HTTP.
BEGIN;

CREATE TABLE usuarios (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    login VARCHAR(254) NOT NULL,
    senha_hash TEXT NOT NULL,
    perfil VARCHAR(20) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT usuarios_nome_valido CHECK (nome = btrim(nome) AND nome <> ''),
    CONSTRAINT usuarios_login_valido CHECK (login = btrim(login) AND login <> ''),
    CONSTRAINT usuarios_hash_preenchido CHECK (btrim(senha_hash) <> ''),
    CONSTRAINT usuarios_perfil_valido CHECK (perfil IN ('ADMINISTRADOR', 'USUARIO'))
);

-- E-mail/login e tratado como um unico identificador de acesso.
-- Ignorar maiusculas e minusculas e uma convencao tecnica desta implementacao.
CREATE UNIQUE INDEX usuarios_login_unico ON usuarios (lower(login));

CREATE TABLE ambientes (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    descricao TEXT,
    tipo VARCHAR(10) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ambientes_nome_valido CHECK (nome = btrim(nome) AND nome <> ''),
    CONSTRAINT ambientes_tipo_valido CHECK (tipo IN ('SALA', 'SETOR'))
);

CREATE UNIQUE INDEX ambientes_nome_unico ON ambientes (lower(nome));

-- Cada linha representa uma atribuicao. A PK composta impede duplicacoes.
CREATE TABLE usuario_ambiente (
    usuario_id BIGINT NOT NULL,
    ambiente_id BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, ambiente_id),
    CONSTRAINT atribuicao_usuario_fk FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id) ON DELETE RESTRICT,
    CONSTRAINT atribuicao_ambiente_fk FOREIGN KEY (ambiente_id)
        REFERENCES ambientes (id) ON DELETE RESTRICT
);

CREATE INDEX atribuicoes_por_ambiente ON usuario_ambiente (ambiente_id);

COMMENT ON TABLE usuarios IS 'Contas do STOCKA; a retirada de acesso ocorre por ativo=false.';
COMMENT ON COLUMN usuarios.senha_hash IS 'Hash da senha. A senha original nao deve ser persistida.';
COMMENT ON TABLE ambientes IS 'Cadastro unico de salas e setores, sem hierarquia.';
COMMENT ON TABLE usuario_ambiente IS 'Atribuicoes explicitas. Administradores nao dependem de atribuicao para acesso geral.';

COMMIT;
