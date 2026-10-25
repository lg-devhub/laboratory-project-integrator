-- RF31/RF32/RN13: servicos realizados, com vinculo opcional a OS do equipamento.
BEGIN;
CREATE TABLE IF NOT EXISTS public.manutencoes (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    equipamento_id BIGINT NOT NULL REFERENCES public.itens(id) ON DELETE RESTRICT,
    ordem_servico_id BIGINT REFERENCES public.ordens_servico(id) ON DELETE RESTRICT,
    data_realizacao DATE NOT NULL,
    descricao_servico TEXT NOT NULL CHECK (btrim(descricao_servico)<>''),
    responsavel_tecnico VARCHAR(120) NOT NULL CHECK (btrim(responsavel_tecnico)<>''),
    usuario_id BIGINT NOT NULL REFERENCES public.usuarios(id) ON DELETE RESTRICT,
    registrado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS manutencoes_por_equipamento ON public.manutencoes(equipamento_id,data_realizacao DESC,id DESC);
CREATE INDEX IF NOT EXISTS manutencoes_por_os ON public.manutencoes(ordem_servico_id);
CREATE OR REPLACE FUNCTION public.validar_vinculos_manutencao() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    PERFORM id FROM public.itens WHERE id=NEW.equipamento_id AND equipamento
        AND forma_registro='INDIVIDUAL' AND excluido_em IS NULL FOR SHARE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Manutencao exige equipamento individual nao excluido' USING ERRCODE='23514'; END IF;
    IF NEW.ordem_servico_id IS NOT NULL THEN
        PERFORM id FROM public.ordens_servico WHERE id=NEW.ordem_servico_id
            AND equipamento_id=NEW.equipamento_id FOR SHARE;
        IF NOT FOUND THEN RAISE EXCEPTION 'OS deve pertencer ao equipamento da manutencao' USING ERRCODE='23514'; END IF;
    END IF;
    RETURN NEW;
END;
$$;
DROP TRIGGER IF EXISTS manutencoes_validar_vinculos ON public.manutencoes;
CREATE TRIGGER manutencoes_validar_vinculos BEFORE INSERT OR UPDATE ON public.manutencoes
    FOR EACH ROW EXECUTE FUNCTION public.validar_vinculos_manutencao();
COMMENT ON TABLE public.manutencoes IS 'Servicos realizados; tecnico informado e autor do lancamento da sessao sao distintos. RF31/RF32/RN13.';
COMMIT;
