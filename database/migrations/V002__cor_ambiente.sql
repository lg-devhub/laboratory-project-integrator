-- Melhoria visual aprovada pelo grupo, nao um requisito funcional do ERS.
-- Reexecutavel para atualizar volumes existentes sem apagar dados.
BEGIN;
ALTER TABLE public.ambientes ADD COLUMN IF NOT EXISTS cor VARCHAR(12) NOT NULL DEFAULT 'TERRACOTA'
    CONSTRAINT ambientes_cor_valida CHECK (cor IN ('TERRACOTA','AZUL','VERDE','VIOLETA','GRAFITE'));
COMMENT ON COLUMN public.ambientes.cor IS 'Identidade visual compartilhada; nao indica status nem permissao.';
COMMIT;
