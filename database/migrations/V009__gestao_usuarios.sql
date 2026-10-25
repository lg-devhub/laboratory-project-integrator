-- RF02/RF04/RF07/RN14/RNF08. Contas desativadas mantem identidade e referencias.
BEGIN;
ALTER TABLE public.usuarios ADD COLUMN IF NOT EXISTS versao BIGINT NOT NULL DEFAULT 1 CHECK(versao>0);
CREATE OR REPLACE FUNCTION public.validar_atualizacao_usuario() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.ativo AND OLD.perfil='ADMINISTRADOR' AND (NOT NEW.ativo OR NEW.perfil<>'ADMINISTRADOR') THEN
        LOCK TABLE public.usuarios IN SHARE ROW EXCLUSIVE MODE;
        PERFORM id FROM public.usuarios WHERE ativo AND perfil='ADMINISTRADOR' AND id<>OLD.id;
        IF NOT FOUND THEN RAISE EXCEPTION 'O ultimo administrador ativo deve ser preservado'
            USING ERRCODE='23514',CONSTRAINT='usuarios_ultimo_admin_ativo'; END IF;
    END IF;
    IF ROW(NEW.nome,NEW.login,NEW.senha_hash,NEW.perfil,NEW.ativo) IS DISTINCT FROM
       ROW(OLD.nome,OLD.login,OLD.senha_hash,OLD.perfil,OLD.ativo) THEN NEW.versao:=OLD.versao+1;
    ELSE NEW.versao:=OLD.versao; END IF;
    RETURN NEW;
END;
$$;
DROP TRIGGER IF EXISTS usuarios_validar_atualizacao ON public.usuarios;
CREATE TRIGGER usuarios_validar_atualizacao BEFORE UPDATE ON public.usuarios
    FOR EACH ROW EXECUTE FUNCTION public.validar_atualizacao_usuario();
COMMENT ON COLUMN public.usuarios.versao IS 'Controle de edicao concorrente, inclusive apos troca da propria senha.';
COMMIT;
