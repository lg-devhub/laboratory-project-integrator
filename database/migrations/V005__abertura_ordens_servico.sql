-- RF28 e primeira consulta de RF29. O ciclo de atualizacao sera outro incremento.
BEGIN;
CREATE TABLE IF NOT EXISTS public.ordens_servico (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    equipamento_id BIGINT NOT NULL REFERENCES public.itens(id) ON DELETE RESTRICT,
    solicitante_id BIGINT NOT NULL REFERENCES public.usuarios(id) ON DELETE RESTRICT,
    problema VARCHAR(2000) NOT NULL CHECK (problema=btrim(problema) AND problema<>''),
    prioridade VARCHAR(5) NOT NULL CHECK (prioridade IN ('BAIXA','MEDIA','ALTA')),
    status VARCHAR(16) NOT NULL DEFAULT 'ABERTA' CHECK (status='ABERTA'),
    aberta_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS ordens_servico_por_equipamento ON public.ordens_servico(equipamento_id,id);

CREATE OR REPLACE FUNCTION public.validar_abertura_os() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    PERFORM id FROM public.itens WHERE id=NEW.equipamento_id AND equipamento
        AND forma_registro='INDIVIDUAL' AND excluido_em IS NULL FOR SHARE;
    IF NOT FOUND THEN RAISE EXCEPTION 'OS exige equipamento individual nao excluido' USING ERRCODE='23514'; END IF;
    RETURN NEW;
END;
$$;
DROP TRIGGER IF EXISTS os_validar_abertura ON public.ordens_servico;
CREATE TRIGGER os_validar_abertura BEFORE INSERT ON public.ordens_servico
    FOR EACH ROW EXECUTE FUNCTION public.validar_abertura_os();
COMMENT ON TABLE public.ordens_servico IS 'OS por equipamento; abertura e consulta. Atualizacao/encerramento ainda indisponiveis.';
COMMIT;
