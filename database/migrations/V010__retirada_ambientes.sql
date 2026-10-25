-- RF13/RN05: retirada de uso preserva cadastro e referencias.
BEGIN;
ALTER TABLE public.ambientes ADD COLUMN IF NOT EXISTS versao BIGINT NOT NULL DEFAULT 1 CHECK (versao>0);

CREATE OR REPLACE FUNCTION public.validar_atualizacao_ambiente() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    -- Itens excluidos logicamente ainda mantem seus vinculos neste modelo.
    IF OLD.ativo AND NOT NEW.ativo AND EXISTS (
        SELECT 1 FROM public.itens WHERE ambiente_id=OLD.id OR setor_responsavel_id=OLD.id
    ) THEN
        RAISE EXCEPTION 'Ambiente possui vinculos de localizacao ou responsabilidade'
            USING ERRCODE='23514',CONSTRAINT='ambientes_vinculos_existentes';
    END IF;
    IF OLD.tipo='SETOR' AND NEW.tipo<>'SETOR' AND EXISTS (
        SELECT 1 FROM public.itens WHERE setor_responsavel_id=OLD.id
    ) THEN
        RAISE EXCEPTION 'Setor responsavel nao pode ser convertido em sala'
            USING ERRCODE='23514',CONSTRAINT='ambientes_setor_responsavel';
    END IF;
    IF ROW(NEW.nome,NEW.tipo,NEW.descricao,NEW.cor,NEW.ativo) IS DISTINCT FROM
       ROW(OLD.nome,OLD.tipo,OLD.descricao,OLD.cor,OLD.ativo) THEN NEW.versao:=OLD.versao+1;
    ELSE NEW.versao:=OLD.versao; END IF;
    RETURN NEW;
END;
$$;
DROP TRIGGER IF EXISTS ambientes_validar_atualizacao ON public.ambientes;
CREATE TRIGGER ambientes_validar_atualizacao BEFORE UPDATE ON public.ambientes
    FOR EACH ROW EXECUTE FUNCTION public.validar_atualizacao_ambiente();
COMMENT ON COLUMN public.ambientes.versao IS 'Confirma retirada somente da versao apresentada ao administrador.';
COMMIT;
