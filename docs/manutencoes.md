# Registro de manutenções e leitura do inventário

## Uso

Abra um equipamento individual e localize Histórico de manutenção. Use Registrar
manutenção, informe data de realização, descrição do serviço e responsável técnico.
O vínculo com uma OS é opcional; a lista apresenta somente OS do equipamento.
A data do serviço é informada pelo usuário; a data de lançamento e o autor são automáticos.
O técnico pode ser externo e não precisa ser uma conta do STOCKA.

O histórico apresenta primeiro a data de realização mais recente; em empate, o
registro mais recente. Mostra serviço, técnico, vínculo com OS, autor e momento
do lançamento. Cancelar não grava dados; erros de validação preservam o rascunho.
Uma falha ao carregar as opções de OS não impede cadastrar um serviço sem vínculo.

Equipamentos Inativos ou Em Manutenção também podem receber registros. O ERS não
define bloqueio por esses status. Registrar manutenção não altera o item, não
conclui OS e não transforma auditoria de cadastro em histórico de serviço.
Concluir uma OS também não gera manutenção automaticamente.

## Requisitos e escolhas técnicas

- RF31/RF32/RN13: registrar/consultar serviços de equipamentos individuais, com
  técnico, data, descrição, autor automático e vínculo opcional com OS.
- RN10: lote e item individual sem classificação de equipamento não recebem manutenção.
- RF06/RN17: consulta e cadastro seguem o acesso ao ambiente atual do equipamento.
  O setor responsável não concede acesso ao inventário.
- RNF08: autorização e gravação na mesma transação, com validação de vínculo no banco.

Decisões técnicas: descrição de até 2000 caracteres, técnico de até 120 caracteres,
data ISO com ano entre 1 e 9999, técnico em texto livre, formulário nos detalhes,
ordenação por data/id e registrado_em. Não há bloqueio adicional de data futura,
exigência de OS concluída nem vínculo obrigatório com OS.
Não foram implementadas edição/exclusão de manutenção ou classificação preventiva/corretiva.

## Modelo e API

V007__manutencoes.sql segue os nomes do guia de diagramas. Adiciona manutencoes,
sem modificar dados de tabelas anteriores:

| Campo | Tipo / papel |
|---|---|
| id | bigint, identity, PK |
| equipamento_id | bigint, FK obrigatória para itens.id |
| ordem_servico_id | bigint, FK opcional para ordens_servico.id |
| data_realizacao | date, obrigatória |
| descricao_servico | text, obrigatória |
| responsavel_tecnico | varchar(120), obrigatório |
| usuario_id | bigint, FK obrigatória para usuarios.id |
| registrado_em | timestamptz, automático |

O trigger rejeita item excluído/não classificado como equipamento e OS de outro
equipamento. As FKs preservam referências. A API obtém equipamento da rota,
autor da sessão e momento do lançamento do PostgreSQL, ignorando dados forjados
para esses campos no JSON.

Base: /api/ambientes/{ambienteId}/itens/{itemId}/manutencoes.

| Método | Resposta |
|---|---|
| GET na base | Lista autorizada; [] quando vazia. |
| POST na base | dataRealizacao, descricaoServico, responsavelTecnico e ordemServicoId opcional; 201 com registro e Location. |
| GET /{id} | Registro desse equipamento; 404 se indisponível. |

Sem sessão: 401. Ambiente/item indisponível ou sem acesso: 404. Campos, data ou
vínculo inválidos e item não classificado como equipamento: 400.
CORS e cabeçalho de escrita seguem a base existente.
ItemDAO centraliza a autorização de equipamento usada por OS e manutenção.
Não há campo ambiente_id duplicado em manutencoes: a consulta acompanha o equipamento.

## Indicadores visuais autorizados nesta etapa

InventarioResumo.vue apresenta número de registros de todo o ambiente, seus status,
equipamentos e registros por quantidade. Os cartões de status filtram a lista;
os totais continuam sendo do ambiente inteiro, mesmo durante pesquisa/filtro.
Não soma quantidades de materiais diferentes como se fossem uma medida única.
A quantidade informada aparece em cada linha, com distinção de unidade individual.

As OS têm contadores separados: abertas, em andamento e concluídas. A tabela de
itens também mostra as contagens por equipamento. Item sem controle patrimonial
mostra Não se aplica; equipamento sem OS mostra Sem OS, somente após consulta válida.
Em falha, mostra Indisponível e oferece nova tentativa, sem representar falha como zero.
Abrir/atualizar OS no diálogo atualiza os indicadores da lista, sem recarga completa.
As cores são acompanhadas de texto e as prioridades de OS também receberam destaque.

GET /api/ambientes/{ambienteId}/resumo-os retorna registros com itemId, abertas,
emAndamento e concluidas. Uma consulta agrupada conta somente OS de equipamentos
não excluídos atualmente localizados no ambiente autorizado. Equipamentos sem OS
não precisam de linha no resultado. A consulta não concede acesso a outros ambientes.
Interface protege resultados de consultas antigas ao trocar de rota ou recarregar.

Esses indicadores são melhoria de UI/UX aprovada pelo usuário, não novos requisitos
oficiais nem gráficos com dados fictícios. Status do item e da OS continuam independentes.

## Executar e verificar

Atualizar-Banco.ps1/Iniciar-Banco.ps1 aplicam V002 a V007. Em instalação nova, compose
também monta V007. Não recrie volumes para atualizar o banco. Reinicie a API após
atualizar o código. Frontend mantém npm run dev.

Testar-Inventario.ps1 executa 43 verificações descartáveis: 19 de inventário,
quatro de senha, 12 de OS e oito de manutenção/resumo. V007 é reaplicada para testar
reexecução. Os casos cobrem conta/autoria, vínculos, validações, histórico, elegibilidade,
independência de status e contagens seguindo a localização atual.
Testar-Backend.ps1 inclui também os 35 testes antigos, totalizando 78 disponíveis.
Os testes não inserem manutenções fictícias no banco principal.
