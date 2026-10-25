-- RF30/RN11/RN12/RNF08: ciclo da OS, encerramento protegido e versao.
BEGIN;
ALTER TABLE public.ordens_servico ADD COLUMN IF NOT EXISTS descricao_atendimento VARCHAR(2000);
ALTER TABLE public.ordens_servico ADD COLUMN IF NOT EXISTS fechada_em TIMESTAMPTZ;
ALTER TABLE public.ordens_servico ADD COLUMN IF NOT EXISTS versao BIGINT NOT NULL DEFAULT 1 CHECK (versao>0);
ALTER TABLE public.ordens_servico DROP CONSTRAINT IF EXISTS ordens_servico_status_check;
ALTER TABLE public.ordens_servico ADD CONSTRAINT ordens_servico_status_check
    CHECK (status IN ('ABERTA','EM_ANDAMENTO','CONCLUIDA'));
ALTER TABLE public.ordens_servico DROP CONSTRAINT IF EXISTS os_encerramento_coerente;
ALTER TABLE public.ordens_servico ADD CONSTRAINT os_encerramento_coerente CHECK (
    (status='CONCLUIDA' AND fechada_em IS NOT NULL AND descricao_atendimento IS NOT NULL AND btrim(descricao_atendimento)<>'')
    OR (status<>'CONCLUIDA' AND fechada_em IS NULL AND descricao_atendimento IS NULL)
);

CREATE OR REPLACE FUNCTION public.validar_abertura_os() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    PERFORM id FROM public.itens WHERE id=NEW.equipamento_id AND equipamento
        AND forma_registro='INDIVIDUAL' AND excluido_em IS NULL FOR SHARE;
    IF NOT FOUND THEN RAISE EXCEPTION 'OS exige equipamento individual nao excluido' USING ERRCODE='23514'; END IF;
    IF NEW.status<>'ABERTA' OR NEW.fechada_em IS NOT NULL OR NEW.descricao_atendimento IS NOT NULL OR NEW.versao<>1 THEN
        RAISE EXCEPTION 'OS deve nascer Aberta' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION public.validar_atualizacao_os() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.status='CONCLUIDA' THEN RAISE EXCEPTION 'OS concluida nao pode ser alterada' USING ERRCODE='23514'; END IF;
    IF ROW(NEW.id,NEW.equipamento_id,NEW.solicitante_id,NEW.aberta_em) IS DISTINCT FROM
       ROW(OLD.id,OLD.equipamento_id,OLD.solicitante_id,OLD.aberta_em) THEN
        RAISE EXCEPTION 'Identidade e abertura da OS devem ser preservadas' USING ERRCODE='23514';
    END IF;
    IF NEW.status<>OLD.status AND NOT (
        (OLD.status='ABERTA' AND NEW.status='EM_ANDAMENTO') OR
        (OLD.status='EM_ANDAMENTO' AND NEW.status='CONCLUIDA')
    ) THEN RAISE EXCEPTION 'Transicao de OS invalida' USING ERRCODE='23514'; END IF;
    IF NEW.status='CONCLUIDA' THEN
        IF NEW.descricao_atendimento IS NULL OR btrim(NEW.descricao_atendimento)='' THEN
            RAISE EXCEPTION 'Descricao do atendimento obrigatoria' USING ERRCODE='23514';
        END IF;
        NEW.fechada_em:=CURRENT_TIMESTAMP;
    ELSE
        IF NEW.descricao_atendimento IS NOT NULL THEN RAISE EXCEPTION 'Atendimento pertence ao encerramento' USING ERRCODE='23514'; END IF;
        NEW.fechada_em:=NULL;
    END IF;
    IF ROW(NEW.problema,NEW.prioridade,NEW.status,NEW.descricao_atendimento) IS DISTINCT FROM
       ROW(OLD.problema,OLD.prioridade,OLD.status,OLD.descricao_atendimento) THEN
        NEW.versao:=OLD.versao+1;
    ELSE NEW.versao:=OLD.versao; END IF;
    RETURN NEW;
END;
$$;
DROP TRIGGER IF EXISTS os_validar_atualizacao ON public.ordens_servico;
CREATE TRIGGER os_validar_atualizacao BEFORE UPDATE ON public.ordens_servico
    FOR EACH ROW EXECUTE FUNCTION public.validar_atualizacao_os();
COMMENT ON TABLE public.ordens_servico IS 'OS: Aberta -> Em Andamento -> Concluida. Concluidas somente para consulta.';
COMMIT;
