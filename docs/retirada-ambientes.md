# Retirada de salas e setores

## Requisito e interpretação

RF13/RN05 permitem ao administrador retirar ambientes de uso, exigindo confirmação
e tratamento prévio dos vínculos. RN05 bloqueia a retirada enquanto houver itens
vinculados, independentemente do status. Referências históricas devem ser preservadas.
O subfluxo A2 de RF13 ainda fala apenas em itens ativos: essa divergência do ERS
permanece registrada em decisões e próximos passos; a implementação segue RN05.

O incremento usa desativação para toda retirada autorizada, mesmo em ambiente
vazio. É uma decisão técnica para preservar identidade e atribuições, sem criar
uma segunda operação de exclusão física. Não há reativação neste incremento.
O nome do ambiente continua reservado.

Vínculo de localização e vínculo de responsabilidade são distintos. Um setor vazio
fisicamente pode continuar responsável por equipamentos em outras salas e não pode
ser retirado. Itens excluídos logicamente também mantêm esses vínculos no modelo
atual e bloqueiam a operação: excluir um item não libera automaticamente a sala.
Isso preserva o caminho de consulta administrativa dos seus históricos.

Movimentação permanece pendente. A retirada não transfere nem exclui itens,
equipamentos, OS ou manutenções automaticamente.

## Interface

No cartão do ambiente, somente o administrador vê **Retirar**. O diálogo explica
a desativação e permite cancelar ou confirmar. Após o sucesso, o cartão sai do hub
e o ambiente deixa de aceitar acesso ao inventário, edição e novas atribuições.
As atribuições já existentes são preservadas, sem conceder acesso ao ambiente inativo.

Se outro administrador editar o ambiente enquanto o diálogo estiver aberto,
a confirmação antiga é recusada. **Atualizar ambientes** fecha o diálogo e recarrega
o hub; é preciso conferir os dados e abrir uma nova confirmação.

A edição comum de ambientes continua disponível. V010 também impede converter um
SETOR em SALA enquanto ele for responsável por equipamentos. Renomear ou mudar a
cor desse setor continua permitido. Essa proteção mantém a regra já existente de
que o responsável por equipamento precisa ser um SETOR ativo (RF18/RN09).

## API e banco

DELETE /api/ambientes/{id}?versao=N exige administrador ativo, cookie de sessão e
X-STOCKA-Request: true. Retorna 204 em sucesso; não apaga a linha.

| Resposta | Significado |
|---|---|
| 400 | Versão ausente, inválida ou não positiva. |
| 401/403 | Sessão ausente/inativa ou perfil sem permissão. |
| 404 | Ambiente inexistente ou já desativado. |
| 409 AMBIENTE_ALTERADO | Confirmação desatualizada; recarregar e confirmar novamente. |
| 409 AMBIENTE_COM_VINCULOS | Existe localização ou responsabilidade de item/equipamento. |
| 409 SETOR_RESPONSAVEL | Edição tentou converter setor responsável em sala. |

V010 adiciona ambientes.versao BIGINT positivo, inicialmente 1, e um trigger de
validação. A versão muda quando nome, tipo, descrição, cor ou ativo mudam de fato.
GET do hub e respostas de cadastro/edição incluem a versão. Neste incremento ela
protege a confirmação de retirada; a edição comum mantém seu contrato anterior.

AmbienteDAO confere novamente o administrador e bloqueia o ambiente na transação
antes de desativá-lo. O cadastro de itens bloqueia o mesmo ambiente para leitura;
assim uma inclusão concorrente termina antes da verificação de vínculos, ou encontra
o ambiente já inativo. O trigger verifica as dependências também em atualização SQL.
Nenhuma coluna de autoria histórica ou tabela de itens é apagada.

## Testes

RetiradaAmbienteLocalTest acrescenta oito testes em banco descartável: autorização,
cabeçalho/versão, retirada e preservação, itens de qualquer status e excluídos,
responsabilidade em outro ambiente, conversão de tipo, confirmação desatualizada,
proteção SQL e inclusão concorrente. O conjunto Testar-Inventario.ps1 contém
67 testes; Testar-Backend.ps1 oferece também os 35 anteriores, totalizando 102.

Em 25/10/2026, os 67 testes do banco descartável passaram e o frontend compilou.
A conferência no navegador cobriu cancelar/retirar, recarga, bloqueios por
localização/responsabilidade, conversão de tipo e conflito com nova confirmação.
As larguras 1440, 390 e 320 pixels foram verificadas sem erros JavaScript.
V010 foi aplicada no banco de trabalho; a comparação das sete tabelas antes/depois
confirmou preservação dos dados, desconsiderando apenas o novo ambientes.versao.
