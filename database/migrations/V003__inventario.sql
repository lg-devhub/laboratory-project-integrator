-- RF17/RF18/RF22/RF23/RF25: inventario e auditoria do cadastro.
-- Reexecutavel; nao insere itens ficticios nem altera os dados existentes.
BEGIN;

CREATE TABLE IF NOT EXISTS public.itens (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ambiente_id BIGINT NOT NULL REFERENCES public.ambientes(id) ON DELETE RESTRICT,
    nome VARCHAR(120) NOT NULL CHECK (nome = btrim(nome) AND nome <> ''),
    categoria VARCHAR(120) NOT NULL CHECK (categoria = btrim(categoria) AND categoria <> ''),
    forma_registro VARCHAR(12) NOT NULL CHECK (forma_registro IN ('INDIVIDUAL','QUANTIDADE')),
    equipamento BOOLEAN NOT NULL DEFAULT FALSE,
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    status VARCHAR(16) NOT NULL CHECK (status IN ('ATIVO','EM_MANUTENCAO','INATIVO')),
    numero_patrimonio VARCHAR(120),
    setor_responsavel_id BIGINT REFERENCES public.ambientes(id) ON DELETE RESTRICT,
    descricao TEXT,
    localizacao VARCHAR(300),
    foto BYTEA,
    foto_tipo VARCHAR(10),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    excluido_em TIMESTAMPTZ,
    CONSTRAINT itens_forma_quantidade CHECK (forma_registro <> 'INDIVIDUAL' OR quantidade = 1),
    CONSTRAINT itens_equipamento_individual CHECK (NOT equipamento OR forma_registro = 'INDIVIDUAL'),
    CONSTRAINT itens_dados_patrimoniais CHECK (
        (equipamento AND numero_patrimonio IS NOT NULL AND btrim(numero_patrimonio) <> ''
         AND numero_patrimonio = btrim(numero_patrimonio) AND setor_responsavel_id IS NOT NULL)
        OR (NOT equipamento AND numero_patrimonio IS NULL AND setor_responsavel_id IS NULL)
    ),
    CONSTRAINT itens_foto_valida CHECK (
        (foto IS NULL AND foto_tipo IS NULL)
        OR (foto IS NOT NULL AND foto_tipo IS NOT NULL AND foto_tipo IN ('image/png','image/jpeg')
            AND octet_length(foto) BETWEEN 1 AND 2097152)
    )
);

-- O indice inclui registros excluidos: patrimonio nunca pode ser reutilizado (RN09).
CREATE UNIQUE INDEX IF NOT EXISTS itens_patrimonio_unico ON public.itens(lower(numero_patrimonio));
CREATE INDEX IF NOT EXISTS itens_por_ambiente ON public.itens(ambiente_id);
CREATE INDEX IF NOT EXISTS itens_por_responsavel ON public.itens(setor_responsavel_id);

CREATE TABLE IF NOT EXISTS public.item_historico (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    item_id BIGINT NOT NULL REFERENCES public.itens(id) ON DELETE RESTRICT,
    usuario_id BIGINT NOT NULL REFERENCES public.usuarios(id) ON DELETE RESTRICT,
    acao VARCHAR(30) NOT NULL CHECK (acao IN ('CADASTRO','EDICAO','ALTERACAO_STATUS','EXCLUSAO')),
    data_hora TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS historico_por_item ON public.item_historico(item_id,id);

CREATE OR REPLACE FUNCTION public.validar_vinculos_item() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    PERFORM id FROM public.ambientes WHERE id=NEW.ambiente_id AND ativo FOR SHARE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Ambiente ativo obrigatorio' USING ERRCODE='23514'; END IF;
    IF NEW.equipamento THEN
        PERFORM id FROM public.ambientes WHERE id=NEW.setor_responsavel_id AND ativo AND tipo='SETOR' FOR SHARE;
        IF NOT FOUND THEN RAISE EXCEPTION 'Setor responsavel ativo obrigatorio' USING ERRCODE='23514'; END IF;
    END IF;
    RETURN NEW;
END;
$$;
DROP TRIGGER IF EXISTS itens_validar_vinculos ON public.itens;
CREATE TRIGGER itens_validar_vinculos BEFORE INSERT OR UPDATE ON public.itens
    FOR EACH ROW EXECUTE FUNCTION public.validar_vinculos_item();

-- A identidade e definida pela API a partir da sessao, somente nesta transacao.
-- Se a auditoria falhar, o INSERT do item inteiro e revertido pelo PostgreSQL.
CREATE OR REPLACE FUNCTION public.auditar_cadastro_item() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE autor BIGINT;
BEGIN
    autor := NULLIF(current_setting('stocka.usuario_id',true),'')::BIGINT;
    IF autor IS NULL THEN RAISE EXCEPTION 'Autor autenticado obrigatorio' USING ERRCODE='23514'; END IF;
    INSERT INTO public.item_historico(item_id,usuario_id,acao) VALUES (NEW.id,autor,'CADASTRO');
    RETURN NEW;
END;
$$;
DROP TRIGGER IF EXISTS itens_auditar_cadastro ON public.itens;
CREATE TRIGGER itens_auditar_cadastro AFTER INSERT ON public.itens
    FOR EACH ROW EXECUTE FUNCTION public.auditar_cadastro_item();

CREATE OR REPLACE FUNCTION public.impedir_alteracao_auditoria() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Historico de itens e imutavel' USING ERRCODE='23514';
END;
$$;
DROP TRIGGER IF EXISTS historico_imutavel ON public.item_historico;
CREATE TRIGGER historico_imutavel BEFORE UPDATE OR DELETE OR TRUNCATE ON public.item_historico
    FOR EACH STATEMENT EXECUTE FUNCTION public.impedir_alteracao_auditoria();

COMMENT ON TABLE public.item_historico IS 'Auditoria de alteracoes; nao e historico de manutencao. RN04/RF22.';
COMMENT ON COLUMN public.itens.excluido_em IS 'Reservado para exclusao logica futura; patrimonio permanece unico.';
COMMIT;
