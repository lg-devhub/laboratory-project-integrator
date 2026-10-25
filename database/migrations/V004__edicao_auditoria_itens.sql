-- RF19/RF21/RF22/RNF08: edicao com revisao e auditoria atomica.
BEGIN;
ALTER TABLE public.itens ADD COLUMN IF NOT EXISTS versao BIGINT NOT NULL DEFAULT 1 CHECK (versao > 0);
ALTER TABLE public.item_historico ADD COLUMN IF NOT EXISTS status_anterior VARCHAR(16);
ALTER TABLE public.item_historico ADD COLUMN IF NOT EXISTS status_novo VARCHAR(16);

CREATE OR REPLACE FUNCTION public.atualizar_versao_item() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF ROW(NEW.nome,NEW.categoria,NEW.forma_registro,NEW.equipamento,NEW.quantidade,NEW.status,
           NEW.numero_patrimonio,NEW.ambiente_id,NEW.setor_responsavel_id,NEW.descricao,NEW.localizacao,
           NEW.foto,NEW.foto_tipo,NEW.excluido_em)
       IS DISTINCT FROM
       ROW(OLD.nome,OLD.categoria,OLD.forma_registro,OLD.equipamento,OLD.quantidade,OLD.status,
           OLD.numero_patrimonio,OLD.ambiente_id,OLD.setor_responsavel_id,OLD.descricao,OLD.localizacao,
           OLD.foto,OLD.foto_tipo,OLD.excluido_em) THEN
        NEW.versao := OLD.versao + 1;
    ELSE
        NEW.versao := OLD.versao;
    END IF;
    RETURN NEW;
END;
$$;
DROP TRIGGER IF EXISTS itens_atualizar_versao ON public.itens;
CREATE TRIGGER itens_atualizar_versao BEFORE UPDATE ON public.itens
    FOR EACH ROW EXECUTE FUNCTION public.atualizar_versao_item();

CREATE OR REPLACE FUNCTION public.auditar_edicao_item() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE autor BIGINT; evento VARCHAR(30);
BEGIN
    IF NEW.versao=OLD.versao THEN RETURN NEW; END IF;
    autor := NULLIF(current_setting('stocka.usuario_id',true),'')::BIGINT;
    IF autor IS NULL THEN RAISE EXCEPTION 'Autor autenticado obrigatorio' USING ERRCODE='23514'; END IF;
    IF OLD.excluido_em IS NULL AND NEW.excluido_em IS NOT NULL THEN
        evento := 'EXCLUSAO';
    ELSIF ROW(NEW.nome,NEW.categoria,NEW.forma_registro,NEW.equipamento,NEW.quantidade,
               NEW.numero_patrimonio,NEW.ambiente_id,NEW.setor_responsavel_id,NEW.descricao,NEW.localizacao,
               NEW.foto,NEW.foto_tipo,NEW.excluido_em)
          IS NOT DISTINCT FROM
          ROW(OLD.nome,OLD.categoria,OLD.forma_registro,OLD.equipamento,OLD.quantidade,
               OLD.numero_patrimonio,OLD.ambiente_id,OLD.setor_responsavel_id,OLD.descricao,OLD.localizacao,
               OLD.foto,OLD.foto_tipo,OLD.excluido_em) THEN
        evento := 'ALTERACAO_STATUS';
    ELSE
        evento := 'EDICAO';
    END IF;
    INSERT INTO public.item_historico(item_id,usuario_id,acao,status_anterior,status_novo)
    VALUES (NEW.id,autor,evento,
        CASE WHEN NEW.status IS DISTINCT FROM OLD.status THEN OLD.status END,
        CASE WHEN NEW.status IS DISTINCT FROM OLD.status THEN NEW.status END);
    RETURN NEW;
END;
$$;
DROP TRIGGER IF EXISTS itens_auditar_edicao ON public.itens;
CREATE TRIGGER itens_auditar_edicao AFTER UPDATE ON public.itens
    FOR EACH ROW EXECUTE FUNCTION public.auditar_edicao_item();
COMMENT ON COLUMN public.itens.versao IS 'Revisao usada para rejeitar formularios desatualizados (RNF08).';
COMMIT;
