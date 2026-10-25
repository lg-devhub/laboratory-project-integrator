# Banco local do STOCKA

Primeiro incremento executavel do banco PostgreSQL: usuarios, ambientes e atribuicoes.
Este projeto e independente do Supabase existente. Nenhum comando conecta a um banco remoto.

## Iniciar

Requisito: Docker Desktop iniciado com containers Linux.
Abra o PowerShell na raiz do projeto (um nível acima de docs) e execute:

```powershell
.\Iniciar-Banco.ps1 -Testar
```

Na primeira execucao, a imagem oficial PostgreSQL 17 sera baixada, uma senha local
sera gerada em `.env`, e o banco sera criado com dados ficticios. Nas execucoes
seguintes, os dados sao preservados pelo volume `stocka-local_stocka_data`.

## Conectar por uma ferramenta de banco

| Campo | Valor |
|---|---|
| Servidor | localhost |
| Porta | 15432 |
| Banco | stocka |
| Usuario PostgreSQL | stocka |
| Senha PostgreSQL | valor POSTGRES_PASSWORD no arquivo .env |

A conexao fica disponivel apenas neste computador. O usuario PostgreSQL administra
o banco de desenvolvimento; ele nao e o perfil Administrador da aplicacao.

Os usuarios ficticios da aplicacao sao `admin@stocka.local`,
`operador@stocka.local`, `semacesso@stocka.local` e `inativo@stocka.local`.
A senha ficticia e `Stocka123`; o banco armazena somente o hash BCrypt.
Essas contas ja podem autenticar pela API Java; o login e o hub Vue ja estao integrados. Veja [o guia do backend](backend.md).

## Verificar e parar

```powershell
.\Testar-Banco.ps1
.\Parar-Banco.ps1
```

Os testes usam transacao e ROLLBACK, preservando os dados iniciais.
Parar o banco nao exclui o volume. Nao e necessario apagar dados para reiniciar.

## Arquivos

| Arquivo | Papel |
|---|---|
| compose.yaml | Executa o PostgreSQL local com armazenamento persistente. |
| database/migrations/V001__acesso_ambientes.sql | Cria as tres tabelas e suas restricoes. |
| database/seed/local.sql | Insere os exemplos locais com hashes BCrypt. |
| database/consultas/ambientes_autorizados.sql | Mostra a consulta prevista para o hub. |
| database/tests/acesso_ambientes.sql | Verifica restricoes e resultados de acesso. |
| docs/modelo-banco.md | Explica os campos, as PKs, as FKs e os limites deste incremento. |

## Como as alteracoes do banco serao compartilhadas

O SQL inicial fica versionado com o projeto. Os scripts de inicializacao do
container executam somente ao criar um volume vazio: editar V001 depois disso
nao modifica um banco existente. A proxima alteracao devera ter um novo arquivo
versionado e ser aplicada explicitamente; nao apague o volume para atualizar o esquema.
V002, V003, V004, V005 e V006 sao aplicadas explicitamente por Atualizar-Banco.ps1 e tambem por Iniciar-Banco.ps1.
Ainda nao ha um executor geral de migracoes no Java; o script aplica V002–V006, que sao reexecutaveis.

Os dados deste computador nao sincronizam com o Supabase. Seu colega podera
adaptar a estrutura usando os arquivos SQL, separando os exemplos locais.

## Proximo incremento

A API Java e o Vue ja oferecem login, sessao, listagem e gestao administrativa de ambientes e acessos.
O inventario agora oferece listagem, cadastro, fotos e auditoria. Edicao, exclusao,
Manutencao e movimentacoes continuam pendentes. O Vue usa a API Java;
as permissoes dos ambientes e itens sao verificadas no backend.

Referencias: [imagem PostgreSQL](https://hub.docker.com/_/postgres) e
[restricoes PostgreSQL](https://www.postgresql.org/docs/17/ddl-constraints.html).

## Banco existente neste computador

O compose.yaml desta raiz reutiliza explicitamente o volume stocka-local_stocka_data. Ele já foi criado na pasta STOCKA-local. Use agora os scripts desta raiz para administrar o mesmo banco; a pasta antiga fica apenas como referência. Em outro computador, os scripts criam um volume vazio e inicializam os exemplos locais. Os dados de um computador não são copiados pelo volume para outro.

## Interface visual

O script Iniciar-Banco.ps1 também inicia o pgAdmin em http://localhost:15050. O script Parar-Banco.ps1 para ambos os serviços. Veja [o guia do pgAdmin](pgadmin.md) para as credenciais e a navegação pelas tabelas.


## V002: cor de identificação

database/migrations/V002__cor_ambiente.sql acrescenta ambientes.cor, obrigatória
com padrão TERRACOTA e restrita às cinco opções da interface. Ambientes existentes
recebem o padrão; cores já escolhidas são preservadas em reexecuções.

Com o banco já iniciado, execute .\Atualizar-Banco.ps1 na raiz antes de iniciar
a API atualizada. Iniciar-Banco.ps1 também chama essa atualização automaticamente.
Em volume novo, compose monta V001, seed, V002, V003, V004, V005 e V006 nessa ordem. A API requer
V002–V006; executar somente o SQL V001 não é suficiente para esta versão.

Não apague o volume e não altere V001 para atualizar um banco existente. V002 não
substitui tabelas, usuários ou atribuições. Para Supabase, aplique V002 após V001
na mesma estrutura PostgreSQL; o script PowerShell administra somente o Docker local.

## V003 Inventário

V003__inventario.sql cria itens e item_historico com FKs, índices, validação de
vínculos e auditoria automática do cadastro. A foto opcional fica no banco, sem
pasta de uploads externa. Ambiente atual e setor responsável são FKs distintas
para ambientes. O responsável não cria um segundo cadastro de equipamento.

Atualizar-Banco.ps1 aplica V002, V003, V004, V005 e V006 em ordem e não reaplica V001 ou o seed no
banco existente. As cinco atualizações são reexecutáveis. As migrações não inserem itens fictícios;
a carga de demonstração é opcional e separada. Para PostgreSQL remoto equivalente, a sequência de
esquema passa a incluir V003/V004/V005/V006. Mais detalhes e testes estão em [Inventário](inventario.md).

## V004 Edição e auditoria

V004__edicao_auditoria_itens.sql acrescenta versão do item e auditoria de edição/status.
Atualizar-Banco.ps1 reaplica V002, V003, V004, V005 e V006 sem recriar dados existentes.
A carga opcional de exemplos é feita separadamente por Popular-Banco.ps1, com API ativa.
Veja [Edição e demonstração](edicao-e-demonstracao.md).

V005 acrescenta a tabela ordens_servico para abertura e consulta de OS. As migrações
V002 a V006 são aplicadas em ordem por Atualizar-Banco.ps1. Não inserem OS fictícias.
Veja [Ordens de serviço](ordens-servico.md).

V006 acrescenta atendimento, fechamento e versão da OS. Atualizar-Banco.ps1 aplica
V002 a V006 sem apagar os cadastros; concluídas ficam protegidas também no banco.
