# Inventário dos ambientes

Este incremento acrescenta abertura do ambiente, listagem, pesquisa, cadastro,
foto opcional, edição/status e consulta da auditoria. Mantém Vue → API Java → PostgreSQL.
O consenso de localização/responsabilidade e o trabalho futuro estão em
[Decisões e próximos passos](decisoes-e-proximos-passos.md).

## Usar e executar

1. Inicie Docker Desktop e execute Iniciar-Banco.ps1 na raiz. O script aplica V002–V006
   sem apagar o banco existente. Com o banco já ativo, pode usar Atualizar-Banco.ps1.
2. Inicie ou reinicie a API com Iniciar-API.ps1 para carregar as novas rotas.
3. No frontend, execute npm run dev e acesse http://localhost:5173.
4. Entre, use Abrir inventário no cartão do ambiente e clique Cadastrar item.

Uma conta USUARIO pode cadastrar nos ambientes atribuídos. ADMINISTRADOR acessa
todos os ambientes ativos. O inventário inicial pode estar vazio: V003 não insere
itens de exemplo no banco de trabalho. O cadastro persiste no PostgreSQL e aparece
após recarregar a página. A auditoria registra o autor real da sessão.

## Dados e decisões técnicas

| Campo | Regra |
|---|---|
| nome/categoria | Obrigatórios, até 120 caracteres cada. Categoria é texto livre, sem criar um catálogo novo. |
| ambiente atual | Obrigatório, obtido da rota autorizada; não é escolhido pelo corpo JSON. |
| forma de registro | INDIVIDUAL ou QUANTIDADE. Individual tem quantidade 1; por quantidade aceita inteiro positivo. |
| equipamento | Somente registro individual. Exige patrimônio e setor responsável. Um item individual sem controle patrimonial também é permitido. |
| patrimônio | Único sem distinguir maiúsculas, inclusive em registros excluídos futuramente; máximo técnico de 120 caracteres. |
| setor responsável | Ambiente ativo de tipo SETOR, sem duplicar localização e sem conceder acesso ao seu inventário. |
| status | ATIVO, EM_MANUTENCAO ou INATIVO; a interface apresenta os nomes do ERS. |
| descrição/localização | Opcionais; limites técnicos de 2000/300 caracteres. Localização detalha armário, bancada etc. |
| foto | Opcional, PNG/JPEG até 2 MB e 10 megapixels. Guardada no banco junto do item; nenhuma URL externa é carregada. |

As exigências de identificação/vínculo/status/forma de registro vêm de RF17/RF18,
RN03/RN07/RN09/RN10 e RF25. Os limites, o formato das fotos e categoria em texto
livre são escolhas técnicas. Filtrar por status é uma conveniência da interface.
O cadastro por quantidade não implementa o formulário de múltiplos registros de RF24.

## Estrutura

- InventarioView.vue coordena carregamento, filtros, lista e diálogo aberto.
- ItemFormulario.vue cuida dos campos e envia cadastro/edição por services/itens.ts.
- ItemDetalhes.vue mostra os dados, carrega foto e consulta o histórico.
- types/item.ts define os contratos da API. O estado do inventário fica na view;
  nenhuma store ou dependência nova foi criada.
- ItemController verifica sessão e acesso ao ambiente antes de consultar/gravar.
- ItemDados valida os dados; ItemDAO realiza as consultas e a transação de cadastro.
- AmbienteDAO centraliza a consulta do ambiente autorizado, também dentro da transação.
- SetorResponsavelController fornece apenas opções de identificação para o formulário.
- V003 cria itens, item_historico, índices, FKs e triggers de integridade/auditoria.

Os formulários usam dialog nativo, cancelamento, foco, estado de envio e mensagens
de erro. Nome duplicado de item não é proibido; patrimônio duplicado de equipamento
retorna 409 e preserva o formulário. A lista não traz os bytes das fotos.

## API

| Rota | Comportamento |
|---|---|
| GET /api/ambientes/{id} | Dados do ambiente autorizado. |
| GET /api/ambientes/{id}/itens | Lista de itens não excluídos atualmente localizados nele. |
| POST /api/ambientes/{id}/itens | Cadastro e auditoria atômicos; 201 com os dados do item. |
| GET /api/ambientes/{id}/itens/{itemId}/historico | Histórico do item autorizado. |
| GET /api/ambientes/{id}/itens/{itemId}/foto | Foto autenticada como PNG/JPEG. |
| GET /api/itens/setores-responsaveis | Apenas id/nome de setores ativos para contas autenticadas; não libera inventário. |

Ausência de sessão retorna 401. Ambiente inexistente, inativo ou sem permissão
retorna a mesma resposta 404, sem expor seus dados. Validação retorna 400 e
patrimônio repetido retorna 409. Cabeçalho de escrita/CORS/cookie seguem a base
existente. Identidade, data de auditoria e ambiente não vêm de campos do navegador.
Quantidade fracionária é rejeitada também na desserialização do JSON.

## Integridade e testes

O DAO abre uma transação, revalida acesso e define stocka.usuario_id a partir da
sessão somente nessa transação. O trigger de INSERT grava item_historico. Sem autor
ou com falha no histórico, PostgreSQL desfaz o cadastro inteiro. Histórico tem
FKs restritivas e bloqueia UPDATE/DELETE/TRUNCATE; não há rotas de alteração dele.
O usuário administrador da aplicação não recebe ferramentas de administração SQL.
Um administrador do próprio PostgreSQL continua tendo poderes sobre o esquema;
isso é diferente dos perfis da aplicação.

Execute Testar-Backend.ps1 para 35 testes existentes, 19 testes de inventário e quatro de alteração de senha.
Testar-Inventario.ps1 pode ser executado separadamente: cria banco exclusivo com
nome stocka_testes_inventario_ seguido de 12 caracteres, prepara esquema/seed,
executa a API de teste e remove o banco em finally. A guarda recusa o banco de
trabalho. Não desabilita os triggers para limpar auditoria e não altera seus itens.
Interrupção abrupta pode deixar um banco temporário pendente; nunca apague stocka
para limpar testes.

Os testes cobrem autorização, cadastro por usuário atribuído, identidade forjada,
localização/responsabilidade distintas, unicidade patrimonial, validações, foto,
auditoria imutável, rollback por falha de auditoria e revogação durante a sessão.
V003 e V004 também são reaplicadas no banco temporário para verificar reexecução.

Conferência em navegador com Vue/API/PostgreSQL reais e banco temporário: entrada
pelo hub, lista vazia, cadastro de equipamento com localização e responsável
distintos, foto, autoria, persistência após recarga, patrimônio duplicado sem perder
formulário, pesquisa, filtro, cadastro por usuário comum, URL sem atribuição e sessão
expirada. Layout inspecionado em 1440, 390 e 320 pixels; a barra de ferramentas de
desenvolvimento do Vue foi ocultada nas capturas. Os bancos temporários foram removidos,
sem inserir itens ou apagar auditoria no banco de trabalho.

## Limites deste incremento

Não oferece exclusão, múltiplos cadastros por formulário,
manutenção ou movimentação. Os triggers de auditoria das futuras operações
deverão ser implementados junto delas, sem permitir alterações sem auditoria.
excluido_em prepara a preservação do patrimônio; nenhuma exclusão está disponível
na interface. A consulta administrativa dos excluídos também fica pendente.

O histórico mostrado é auditoria de alterações, não histórico de manutenção.
A tabela pode rolar horizontalmente em telas estreitas. Fotos, formulário e detalhes
adaptam-se à largura. Paginação e refinamento visual podem ser discutidos quando
houver volume real de dados; não há gráfico ou contagem fictícia de unidades.

## Edição após o cadastro

GET /api/ambientes/{id}/itens/{itemId} consulta um item autorizado.
PUT na mesma rota edita os campos permitidos, exigindo a versão lida anteriormente.
V004 registra auditoria da atualização e detecta conflitos (409 ITEM_ALTERADO).
Veja [Edição e demonstração](edicao-e-demonstracao.md) para campos, fotos e testes.

Equipamentos agora permitem abrir, consultar, atualizar e concluir [ordens de serviço](ordens-servico.md).
O histórico de alterações do item continua separado dos registros de OS.
