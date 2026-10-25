# Ordens de serviço — abertura, acompanhamento e encerramento

## Usar

No hub, abra o ambiente e os detalhes de um equipamento individual.
Na seção Ordens de serviço, informe problema e prioridade para abrir uma OS.
Solicitante, data de abertura e status Aberta são definidos no servidor.

Expanda uma OS e use Atualizar OS para corrigir problema/prioridade, manter sua
fase ou avançar: Aberta → Em Andamento → Concluída. Para concluir, informe a
descrição do atendimento. O fechamento é registrado automaticamente e a OS
permanece somente para consulta. Não há reabertura nem salto/retorno de fase.
O status do equipamento permanece independente da OS.

Cancelar não grava dados. Erros preservam o rascunho. Durante o envio, o diálogo
bloqueia fechamento/edição do equipamento. Em conflito, preserva os campos e
exige carregar explicitamente a versão atual antes de salvar. Se outra pessoa
já concluiu a OS, permite consultar o resultado.

## Requisitos e decisões

- RF28: abertura com problema/prioridade, solicitante e data automáticos.
- RF29: consulta incluindo fase, abertura e atendimento/fechamento quando existentes.
- RF30/RN11: atualização, avanço sequencial, atendimento obrigatório ao concluir
  e preservação da concluída para consulta.
- RN10: lotes e itens sem classificação de equipamento não recebem OS.
- RN12: operações de OS não alteram automaticamente a condição do equipamento.
- RN17/RF06: acesso pelo ambiente atual do equipamento; responsabilidade não concede
  acesso. Administradores acessam ambientes ativos; usuários precisam de atribuição.
- RNF08: transação e versão protegem contra sobrescrita concorrente.

Decisões técnicas: problema e atendimento limitados a 2000 caracteres; prioridade
Média inicialmente selecionada; problema/prioridade editáveis antes da conclusão;
formulário no equipamento, lista expansível ordenada por identificação decrescente.
Atendimento é preenchido no encerramento, sem rascunho persistente desse campo nas
fases anteriores. O ERS não proíbe múltiplas OS abertas por equipamento.

O passo 3 de RF30 menciona informar a data, mas a descrição do próprio RF e RN11
exigem fechamento automático. A implementação segue essa regra; alinhar o passo
textual na revisão documental do grupo.

## Banco e API

V005 cria ordens_servico; V006 acrescenta atendimento, fechamento, versão e regras
de transição. Atualizar-Banco.ps1/Iniciar-Banco.ps1 aplicam V002–V006 em ordem.
Compose também monta os arquivos na primeira inicialização de volume novo.
Não apague volumes para atualizar. Reinicie a API para carregar as rotas novas.

| Campo | Tipo / vínculo |
|---|---|
| id | bigint, PK gerada |
| equipamento_id | bigint, FK para itens, exclusão física bloqueada |
| solicitante_id | bigint, FK para usuarios, autoria preservada |
| problema | varchar(2000), obrigatório |
| prioridade | varchar(5): BAIXA, MEDIA, ALTA |
| status | varchar(16): ABERTA, EM_ANDAMENTO, CONCLUIDA |
| aberta_em | timestamptz, automático |
| descricao_atendimento | varchar(2000), obrigatório ao concluir |
| fechada_em | timestamptz, automático no encerramento |
| versao | bigint positivo, incrementado em mudança efetiva |

O ambiente vem do equipamento, sem duplicação na OS. Triggers verificam equipamento
elegível, transições, identidade, campos de encerramento e imutabilidade das concluídas.
Uma atualização sem mudança efetiva em OS ativa não incrementa a versão.

Base: /api/ambientes/{ambienteId}/itens/{itemId}/ordens-servico.

| Método | Resultado |
|---|---|
| GET na base | Lista autorizada; [] se vazia. |
| POST na base | problema/prioridade; 201 com OS e Location. |
| GET /{ordemId} | Dados de uma OS desse equipamento. |
| PUT /{ordemId} | problema, prioridade, status, descricaoAtendimento e versao; 200 com OS atualizada. |

Sem sessão: 401. Ambiente/item indisponível, excluído ou não autorizado e ambiente
inativo: 404. O status Inativo do equipamento não bloqueia OS. Validação/transição
inválida ou item não classificado como equipamento: 400. Conflito/concluída: 409,
com codigo OS_ALTERADA/OS_CONCLUIDA e ordemAtual, após verificar a autorização.
CORS e cabeçalho de escrita seguem a autenticação existente.

Identidade, equipamento e datas não são escolhidos pelo JSON. Solicitante e abertura
originais são preservados. O DAO verifica acesso, bloqueia equipamento para leitura
e OS para atualização na mesma transação, comparando a versão antes de gravar.
Operações de OS não alteram o item nem sua auditoria RF22. A OS tem solicitante,
abertura e fechamento próprios; não foi criado histórico de cada edição da OS.
Manutenções e central geral de OS continuam para outros incrementos.

## Arquivos e verificação

- OrdensServico.vue: abertura, consulta e coordenação em ItemDetalhes.vue.
- OrdemServicoEdicao.vue: atualização e resolução explícita de conflito.
- services/ordensServico.ts e types/ordemServico.ts: requisições e contratos.
- OrdemServicoController: sessão, rotas e respostas.
- OrdemServicoDados/OrdemServicoEdicao: validação.
- OrdemServicoDAO: autorização e operações transacionais.
- V005/V006: esquema e integridade PostgreSQL.

Testar-Inventario.ps1 executa 35 testes descartáveis: 19 de inventário, quatro de
senha e 12 de OS. Cobrem autoria, datas, permissões, fases, atendimento, imutabilidade,
concorrência, validações, ausência de efeitos no item e consulta após mudar a
localização em fixture SQL (sem implementar movimentação na interface).
As migrações são reaplicadas. Testar-Backend.ps1 inclui os 35 testes anteriores,
totalizando 70 disponíveis. Testes de OS não criam/alteram OS no banco de trabalho.
