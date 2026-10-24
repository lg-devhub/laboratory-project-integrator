\set ON_ERROR_STOP on
BEGIN;

DO $teste$
DECLARE
    v_usuario BIGINT;
    v_ambiente BIGINT;
    v_quantidade INTEGER;
    v_senha TEXT;
BEGIN
    SELECT id, senha_hash INTO STRICT v_usuario, v_senha
    FROM usuarios WHERE login = 'operador@stocka.local';
    SELECT id INTO STRICT v_ambiente FROM ambientes WHERE nome = 'Producao';

    IF v_senha = 'Stocka123' OR crypt('Stocka123', v_senha) <> v_senha
       OR crypt('senha-incorreta', v_senha) = v_senha THEN
        RAISE EXCEPTION 'Falha no hash da senha de teste';
    END IF;
    RAISE NOTICE 'OK: senha de teste em hash; senha correta aceita e incorreta rejeitada';

    BEGIN
        INSERT INTO usuarios (nome, login, senha_hash, perfil)
        VALUES ('Duplicado', 'OPERADOR@stocka.local', 'hash-teste', 'USUARIO');
        RAISE EXCEPTION 'Aceitou login duplicado';
    EXCEPTION WHEN unique_violation THEN NULL;
    END;
    BEGIN
        INSERT INTO usuarios (nome, login, senha_hash, perfil)
        VALUES ('Perfil invalido', 'perfil-teste@stocka.local', 'hash-teste', 'GESTOR');
        RAISE EXCEPTION 'Aceitou perfil invalido';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    BEGIN
        INSERT INTO usuarios (nome, login, senha_hash, perfil)
        VALUES ('', 'nome-teste@stocka.local', 'hash-teste', 'USUARIO');
        RAISE EXCEPTION 'Aceitou nome vazio';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    RAISE NOTICE 'OK: unicidade de login, perfil permitido e nome obrigatorio';

    BEGIN
        INSERT INTO ambientes (nome, tipo) VALUES ('PRODUCAO', 'SETOR');
        RAISE EXCEPTION 'Aceitou nome de ambiente duplicado';
    EXCEPTION WHEN unique_violation THEN NULL;
    END;
    BEGIN
        INSERT INTO ambientes (nome, tipo) VALUES ('Ambiente invalido', 'PREDIO');
        RAISE EXCEPTION 'Aceitou tipo de ambiente invalido';
    EXCEPTION WHEN check_violation THEN NULL;
    END;
    RAISE NOTICE 'OK: nome unico e classificacao Sala/Setor';

    BEGIN
        INSERT INTO usuario_ambiente VALUES (v_usuario, v_ambiente);
        RAISE EXCEPTION 'Aceitou atribuicao duplicada';
    EXCEPTION WHEN unique_violation THEN NULL;
    END;
    BEGIN
        INSERT INTO usuario_ambiente VALUES (-1, v_ambiente);
        RAISE EXCEPTION 'Aceitou usuario inexistente';
    EXCEPTION WHEN foreign_key_violation THEN NULL;
    END;
    BEGIN
        INSERT INTO usuario_ambiente VALUES (v_usuario, -1);
        RAISE EXCEPTION 'Aceitou ambiente inexistente';
    EXCEPTION WHEN foreign_key_violation THEN NULL;
    END;
    RAISE NOTICE 'OK: PK composta e ambas as FKs das atribuicoes';

    -- Esta funcao de teste reproduz a consulta do hub; nao e uma API nem RLS.
    CREATE TEMP TABLE resultados_acesso AS
    SELECT u.login, a.id AS ambiente_id
    FROM usuarios u CROSS JOIN ambientes a
    WHERE u.ativo AND a.ativo AND (
        u.perfil = 'ADMINISTRADOR'
        OR EXISTS (SELECT 1 FROM usuario_ambiente ua
                   WHERE ua.usuario_id = u.id AND ua.ambiente_id = a.id)
    );
    SELECT count(*) INTO v_quantidade FROM resultados_acesso
    WHERE login = 'admin@stocka.local';
    IF v_quantidade <> 2 THEN RAISE EXCEPTION 'Administrador nao recebeu os dois ambientes'; END IF;
    SELECT count(*) INTO v_quantidade FROM resultados_acesso
    WHERE login = 'operador@stocka.local';
    IF v_quantidade <> 1 THEN RAISE EXCEPTION 'Operador recebeu ambientes incorretos'; END IF;
    SELECT count(*) INTO v_quantidade FROM resultados_acesso
    WHERE login IN ('semacesso@stocka.local', 'inativo@stocka.local');
    IF v_quantidade <> 0 THEN RAISE EXCEPTION 'Conta sem atribuicao ou desativada recebeu acesso'; END IF;
    RAISE NOTICE 'OK: consulta do hub para administrador, operador, sem atribuicao e desativado';

    -- Confirma cardinalidade N:N com uma segunda atribuicao temporaria.
    INSERT INTO usuario_ambiente (usuario_id, ambiente_id)
    SELECT v_usuario, id FROM ambientes WHERE nome = 'Sala de manutencao';
    SELECT count(*) INTO v_quantidade FROM usuario_ambiente WHERE usuario_id = v_usuario;
    IF v_quantidade <> 2 THEN RAISE EXCEPTION 'Atribuicao multipla nao funcionou'; END IF;
    RAISE NOTICE 'OK: usuario pode pertencer a mais de um ambiente';

    BEGIN
        DELETE FROM ambientes WHERE id = v_ambiente;
        RAISE EXCEPTION 'Excluiu ambiente com atribuicoes';
    EXCEPTION WHEN foreign_key_violation THEN NULL;
    END;
    RAISE NOTICE 'OK: integridade referencial preservada';
END;
$teste$;

-- O teste desfaz suas alteracoes; os dados locais iniciais permanecem intactos.
ROLLBACK;
\echo 'STOCKA: verificacoes do primeiro incremento concluidas.'
