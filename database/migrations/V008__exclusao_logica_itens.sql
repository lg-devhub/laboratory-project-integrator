-- RF20/RN15: o lock do item serializa abertura de OS e exclusao.
BEGIN;
CREATE OR REPLACE FUNCTION public.validar_exclusao_item() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.excluido_em IS NULL AND NEW.excluido_em IS NOT NULL AND EXISTS (
        SELECT 1 FROM public.ordens_servico WHERE equipamento_id=OLD.id AND status IN ('ABERTA','EM_ANDAMENTO')
    ) THEN RAISE EXCEPTION 'Equipamento possui OS aberta ou em andamento' USING ERRCODE='23514'; END IF;
    RETURN NEW;
END;
$$;
DROP TRIGGER IF EXISTS itens_validar_exclusao ON public.itens;
CREATE TRIGGER itens_validar_exclusao BEFORE UPDATE ON public.itens
    FOR EACH ROW EXECUTE FUNCTION public.validar_exclusao_item();
COMMENT ON COLUMN public.itens.excluido_em IS 'RF20/RN15: exclusao logica com preservacao de patrimonio, foto e historicos.';
COMMIT;
