# Exclusão lógica de itens

## Uso e regras

Nos detalhes de um item, use Excluir item. A confirmação explica a retirada das
listagens normais e a preservação de dados/históricos. Cancelar não grava nada.
Administrador e usuário atribuído ao ambiente podem excluir, conforme RF20/RF06.

O item permanece no PostgreSQL, com excluido_em automático. Seu status anterior,
foto, patrimônio, localização, responsável e históricos são preservados.
Equipamento com OS ABERTA ou EM_ANDAMENTO não pode ser excluído (RN15).
OS concluídas e manutenções não impedem exclusão e continuam preservadas.
O patrimônio não pode ser reutilizado, inclusive entre excluídos (RN09).

A operação gera EXCLUSAO no item_historico com autor da sessão e data automática.
Falha na auditoria desfaz exclusão e alteração de versão. Um formulário desatualizado
recebe conflito, exigindo revisão dos dados antes de outra confirmação (RNF08).
Não há restauração nem exclusão física na aplicação. Consulta somente leitura
dos excluídos é uma decisão de escopo deste incremento.

## Consulta administrativa

No inventário, o administrador pode marcar Consultar itens excluídos deste ambiente.
A listagem passa a mostrar somente excluídos e sua data de exclusão. As contagens
normais não incluem esses registros. Os detalhes permitem consultar foto, auditoria,
OS e manutenções, com ações de escrita indisponíveis.

O usuário comum não pode consultar excluídos, mesmo que anteriormente tivesse
acesso ao equipamento. A API valida esse controle, inclusive por URL direta.
Consulta depende também do ambiente ativo; a retirada/consulta de ambientes fora
de uso será tratada no incremento de ambientes.

## API e integridade

- DELETE /api/ambientes/{ambienteId}/itens/{itemId}?versao={versao}: exige sessão,
  acesso e cabeçalho de escrita; retorna 200 com item excluído.
- GET /api/ambientes/{ambienteId}/itens?excluidos=true: somente administrador.
- GET de item, foto, histórico, OS e manutenções permite consultar excluídos
  somente como administrador autorizado.
- Listas normais e resumo de OS continuam omitindo excluídos.
- Edição, nova OS e nova manutenção em excluído retornam 404, inclusive para admin.

Sem sessão: 401. Lista administrativa solicitada por perfil comum: 403.
Item/ambiente indisponível ou sem acesso: 404. Versão inválida: 400.
OS pendente: 409 OS_PENDENTE. Versão desatualizada: 409 ITEM_ALTERADO, com itemAtual.
Repetir a exclusão de um registro já excluído retorna 404 e não duplica auditoria.
CORS permite DELETE para a origem configurada e exige X-STOCKA-Request nas escritas.

V008__exclusao_logica_itens.sql acrescenta o bloqueio no PostgreSQL, sem novas
colunas. O lock exclusivo do item serializa exclusão com abertura de OS, que
obtém lock compartilhado. Assim não fica equipamento excluído com OS pendente
por uma corrida entre as duas operações.
Atualizar-Banco.ps1 aplica V002 a V010; compose inclui V008 em volume novo.
Não recrie volumes para atualizar.

## Arquivos e verificação

ItemDAO concentra transação e bloqueios; ItemController fornece exclusão e acesso
administrativo. ItemExclusao.vue contém a confirmação. ItemDetalhes.vue apresenta
consulta sem alterações, e InventarioView.vue alterna inventário/excluídos.
OS e manutenção aceitam consulta histórica somente após verificar administrador,
preservando a proibição de novas escritas.

ExclusaoItemLocalTest acrescenta oito testes de autorização, auditoria, fotos,
patrimônio, OS/manutenção, bloqueios, versão, rollback e abertura concorrente de OS.
Testar-Inventario.ps1 executa 51 testes descartáveis; com os 35 anteriores,
Testar-Backend.ps1 oferece 102 verificações. Testes não excluem dados do banco principal.

## Próximas exclusões

Este incremento não implementa retirada de ambientes ou desativação de contas.
RF13/RN05 exigem verificar localização, responsabilidade e referências históricas.
RF07/RN14 exigem preservar autoria e impedir desativação do último administrador.
Essas ações precisam de seus próprios fluxos e validações.
