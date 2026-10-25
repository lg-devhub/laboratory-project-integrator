# Documentação do STOCKA

| Guia | Conteúdo |
|---|---|
| [Guia dos diagramas](guia-diagramas-equipe.md) | Roteiro para casos de uso, classes de domínio, sequências RF28/RF30 e DER completo; separa implementação de propostas futuras. |
| [Decisões e próximos passos](decisoes-e-proximos-passos.md) | Consenso sobre localização/responsabilidade, decisões técnicas e pendências futuras. |
| [Inventário](inventario.md) | Tela interna, cadastro, fotos, auditoria, permissões e testes isolados. |
| [Retirada de ambientes](retirada-ambientes.md) | Desativação de salas/setores, bloqueios por localização/responsabilidade e confirmação. |
| [Exclusão de itens](exclusao-itens.md) | Confirmação, bloqueios, históricos preservados e consulta administrativa. |
| [Manutenções e indicadores](manutencoes.md) | Serviços realizados, OS opcional e contagens reais no inventário. |
| [Ordens de serviço](ordens-servico.md) | Abertura, andamento, encerramento, permissões e conflitos de edição. |
| [Frontend](frontend.md) | Pastas Vue, rotas, store, serviços e execução. |
| [Gestão de usuários](gestao-usuarios.md) | Cadastro, edição, desativação, último administrador e conflitos. |
| [Alteração de senha](alteracao-senha.md) | RF03/RN06, formulário, validações e limites do incremento. |
| [Backend](backend.md) | Conexão, arquivos Java, autenticação, sessão, API, testes e futura hospedagem no Supabase. |
| [Banco local](banco-local.md) | Docker, execução, dados fictícios e arquivos SQL. |
| [Modelo do banco](modelo-banco.md) | Campos, PKs, FKs, convenções técnicas e limites do incremento. |
| [pgAdmin](pgadmin.md) | Interface visual para consultar o banco e explicação dos contêineres. |

Os guias descrevem a versão atual do projeto. O código antigo, os diagramas do
protótipo e os guias substituídos foram preservados em um histórico externo à
pasta de desenvolvimento para não serem confundidos com a implementação atual.

A ERS revisada continua sendo a referência de requisitos. Sua versão aprovada
pelo grupo ainda precisa ser incluída no repositório. Os guias técnicos não
substituem a ERS; escolhas de implementação devem ser distinguidas dos requisitos.

Consulte também [Edição e demonstração](edicao-e-demonstracao.md) para edição de itens,
conflitos, limpeza realizada e população opcional do banco de trabalho.
