# Decisões e próximos passos do STOCKA

Registro das definições acordadas com o responsável pelo frontend em 24/10/2026.
A referência de requisitos continua sendo ERS_STOCKA_Sprint2.docx, versão 1.7,
confrontada com a problemática da empresa e a proposta inicial da demanda.
Este guia distingue decisões de implementação e sugestões futuras dos requisitos.

## Localização e responsabilidade

- Salas e setores usam a mesma tabela ambientes. O campo tipo classifica SALA/SETOR;
  não estabelece hierarquia entre ambientes.
- Todo item pertence a um único ambiente atual, conforme RN03.
- Equipamentos individuais também têm patrimônio único e setor responsável (RF18/RN09).
  O setor responsável deve existir, estar ativo e ter tipo SETOR. Pode ser diferente
  do ambiente em que o equipamento está fisicamente, inclusive uma SALA.
- Se um equipamento é cadastrado em um SETOR, esse mesmo setor pode ser o responsável.
  O formulário oferece essa seleção inicial; o usuário pode alterá-la.
- O inventário de um ambiente lista somente os itens localizados nele. A responsabilidade
  não duplica o cadastro e não faz o equipamento aparecer em dois inventários físicos.
- Escolher um responsável não concede acesso ao inventário desse setor. Pela RN17,
  o acesso ao equipamento e seus históricos acompanha o ambiente atual e as atribuições.
- O seletor de responsabilidade disponibiliza somente id/nome dos setores ativos a
  contas autenticadas. É uma decisão de interface confirmada nesta discussão, sem
  acesso aos inventários, usuários atribuídos ou descrições de outros ambientes.

A proposta de unificar localização e responsabilidade foi substituída pelo consenso
acima. Não remover a distinção nem exigir que uma sala pertença a um setor sem uma
nova definição do grupo. O ERS não define uma árvore de salas dentro de setores.

## Incremento de inventário atual

- Abrir o ambiente a partir do hub, verificando atribuição também na API.
- Listar, pesquisar por identificação/patrimônio e filtrar por status os itens locais.
- Cadastrar item individual ou registro por quantidade; equipamentos são individuais.
- Exigir nome/categoria/quantidade/status; patrimônio e responsável para equipamentos.
- Aceitar descrição, detalhamento de localização e foto opcionais.
- Gravar cadastro e auditoria juntos, com autor da sessão e data do servidor.
- Consultar detalhes, foto e histórico de alterações sob a mesma regra de acesso.
- Editar campos e status com auditoria atômica e detecção de edição concorrente.
- Alterar a própria senha pelo hub, conforme RF03/RN06, para ambos os perfis.
- Preservar identidade e vínculos nesta edição comum; é uma decisão técnica de escopo,
  não uma proibição adicional da empresa. Veja [Edição e demonstração](edicao-e-demonstracao.md).

Isso não encerra todos os requisitos do inventário nem toda a entrega da Sprint 2.
O nome do arquivo ERS é a versão da entrega documental, não um backlog que já foi concluído.

## Próximos incrementos sugeridos

| Etapa | Requisito ou decisão | Cuidados |
|---|---|---|
| Cadastro manual de vários registros | RF24 | Diferente de informar quantidade em um único registro (RF25); definir validação e resultado do conjunto. |
| Gerenciar contas | RF02/RF04/RF07, RN06/RN14 | Login e alteração da própria senha estão disponíveis; faltam as telas de gestão, preservação de autoria e proteção do último administrador. |
| Ordens de serviço | RF28–RF30, RN11/RN12 | Somente equipamentos individuais; ciclo e encerramento conforme ERS, sem alterar automaticamente o status do equipamento. |
| Manutenções | RF31/RF32/RN13 | Data, serviço, técnico, autor automático e vínculo opcional com OS. |
| Movimentação | RF33/RN16/RN17 | Administrador altera ambiente atual, define responsável após a operação e registra origem/destino/data/autor; preserva patrimônio, OS e históricos. Não é importar ou duplicar item. |
| Exclusão lógica de itens | RF20/RN15 | Preservar histórico e patrimônio; bloquear equipamentos com OS aberta/em andamento e permitir consulta administrativa dos excluídos. |
| Retirar ambientes de uso | RF13/RN05 | Verificar localização de todos os itens e vínculos atuais de responsabilidade; preservar referências históricas. Sem atalho de exclusão física no hub. |

A ordem acima é uma sugestão de desenvolvimento incremental; pode ser discutida
antes de iniciar cada etapa. Não apresentar ações ainda indisponíveis como funcionais.

## Sugestões visuais e funcionais para avaliar depois

- Consulta de equipamentos sob responsabilidade de um setor, claramente separada
  do inventário por localização. Deve respeitar o acesso ao ambiente atual; a consulta
  não concede acesso adicional. É uma sugestão, não uma tela exigida nesta etapa.
- Refinar o design com orientações próprias para STOCKA e revisão especializada,
  preservando o estilo industrial aprovado e componentes Vue pequenos.
- Animações discretas e melhorias de interação somente após validar o fluxo funcional.
- Contadores de itens nos cartões poderão usar dados reais quando forem úteis;
  gráficos/dashboards não devem ser usados como preenchimento nem tratados como requisito atual.
- Paleta de cores dos ambientes é melhoria visual aprovada, sem significado de status.

## Pendências documentais

- Levar a versão aprovada do ERS para o repositório; ela ainda está fora da pasta do projeto.
- Ajustar o subfluxo A2 de RF13: RN05 bloqueia retirada com itens independentemente
  do status; o texto do subfluxo ainda menciona somente itens/equipamentos ativos.
- Atualizar diagramas quando o grupo revisar o modelo de inventário, auditoria e
  responsabilidade. Diagramas antigos do protótipo não descrevem a implementação atual.
- Formatos/tamanho da foto, limites de texto, categoria em texto livre, filtro de status
  e organização das telas são decisões técnicas, não exigências novas da empresa.

## Continuidade do trabalho

Consultar este registro e o ERS antes de mudar o modelo ou ampliar funcionalidades.
Implementar incrementos pequenos, explicar os arquivos e validar o fluxo completo.
Manter código e guias coerentes. Commits registram etapas verificadas; envio ao GitHub
deve respeitar a autorização do usuário e eventuais alterações da equipe no remoto.
