-- Exemplo para estudo no psql, executado de dentro do container.
-- O backend usara o ID da sessao autenticada, nunca um login arbitrario enviado
-- pelo frontend para determinar o dono da consulta.
-- Troque apenas o valor abaixo para explorar os quatro usuarios ficticios.
\set login 'operador@stocka.local'

SELECT a.id, a.nome, a.tipo, a.descricao
FROM usuarios u
CROSS JOIN ambientes a
WHERE lower(u.login) = lower(:'login')
  AND u.ativo
  AND a.ativo
  AND (
      u.perfil = 'ADMINISTRADOR'
      OR EXISTS (
          SELECT 1 FROM usuario_ambiente ua
          WHERE ua.usuario_id = u.id AND ua.ambiente_id = a.id
      )
  )
ORDER BY a.nome;
