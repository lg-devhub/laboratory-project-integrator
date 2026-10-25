# Guia para reconstruir os diagramas do STOCKA

Referência: ERS_STOCKA_Sprint2.docx, versão 1.7 consultada nesta conversa, e implementação até o commit 46dcf0c e incrementos locais V008–V010 de exclusão de itens, gestão de usuários e retirada de ambientes. Preparado em 25/10/2026.

Este guia descreve o sistema completo previsto no ERS. Ele separa regras oficiais, estrutura implementada e propostas técnicas para partes ainda não implementadas. Não cria tabelas nem altera código.

## 1. O que será desenhado

Criem cinco diagramas:

1. Casos de uso do STOCKA completo.
2. Classes de domínio do STOCKA completo.
3. Sequência de abertura de OS, RF28.
4. Sequência de atualização/encerramento de OS, RF30.
5. Modelo lógico do banco, chamado de DER neste guia.

As duas sequências de OS são uma recomendação para substituir as figuras antigas de login e edição de ambientes. Confirmem apenas se o professor exige sequências de requisitos específicos.

Classes de domínio explicam os conceitos do sistema. O modelo lógico explica tabelas PostgreSQL. Não são a mesma figura. Este guia não representa todas as classes Java, controllers, DAOs ou componentes Vue.

### Legenda obrigatória de situação

- IMPLEMENTADO: estrutura ou comportamento existente no projeto.
- PARCIAL: parte funciona, mas o requisito ainda tem pendências.
- PLANEJADO: exigido pelo ERS, ainda sem funcionalidade implementada.
- PROPOSTA TÉCNICA: nome, tipo ou organização sugeridos para implementar uma exigência. Não são exigências literais da empresa.

Acrescentem essa situação aos títulos ou em notas. Se usarem cores, mantenham também o texto: a interpretação não deve depender somente da cor.

Ter uma coluna no banco não significa ter a funcionalidade pronta. Por exemplo, ativo e excluido_em existem: a exclusão de itens e a retirada de ambientes já estão disponíveis com as proteções explicadas neste guia.

## 2. Preparação no diagrams.net

1. Abram app.diagrams.net e criem um arquivo novo.
2. Habilitem as bibliotecas UML/UML 2.5 e Entity Relation em Mais formas / More Shapes.
3. Criem uma página para cada diagrama. Se o de casos de uso ficar grande, usem páginas por área, mantendo os mesmos atores e permissões.
4. Usem conectores presos às caixas. Assim, mover a caixa não deixa uma linha solta.
5. Usem conectores ortogonais quando ajudarem a evitar cruzamentos.
6. Salvem o original editável em .drawio. Exportem PDF ou SVG para inserir no documento, conferindo se os textos continuam legíveis.

Nomes de arquivo sugeridos:

- STOCKA_CasosDeUso.drawio
- STOCKA_ClassesDominio.drawio
- STOCKA_Sequencia_RF28.drawio
- STOCKA_Sequencia_RF30.drawio
- STOCKA_DER_Logico.drawio

Esses nomes de arquivo e a escolha das duas sequências são sugestões de organização.

Documentação da ferramenta:

- https://www.drawio.com/docs/diagram-types/entity-relationship-tables/
- https://www.drawio.com/docs/tutorials/crows-foot-notation/
- https://www.drawio.com/docs/diagram-types/uml/sequence-diagrams/

## 3. Vocabulário que todos devem usar

| Termo | Significado no STOCKA |
|---|---|
| Usuário | Conta de acesso. Pode ter perfil ADMINISTRADOR ou USUARIO. |
| Ambiente | Sala ou setor. O tipo classifica o ambiente; não cria hierarquia. |
| Item | Registro do inventário. Pode ser individual ou por quantidade. |
| Equipamento | Item individual marcado para controle patrimonial. Não é uma tabela separada no banco atual. |
| Ambiente atual | Onde o item está localizado. Todo item tem exatamente um. |
| Setor responsável | Ambiente de tipo SETOR responsável pelo equipamento. Pode ser diferente da localização. |
| Histórico do item | Auditoria automática de cadastro, edição, status e exclusão. |
| OS | Ordem de serviço de um equipamento individual. |
| Manutenção | Registro de um serviço realizado no equipamento. Não é a própria OS. |
| Movimentação | Registro de mudança administrativa de localização/responsabilidade do equipamento. Não cria cópia dele. |

Exemplo: a furadeira está na Sala de ferramentas, mas o Setor de manutenção é responsável por ela. Existe um item e dois vínculos diferentes. A furadeira aparece no inventário da sala, não é duplicada no inventário do setor responsável.

Não criem Turma, Curso, Departamento, aprovação de requisição, preço ou custo como requisitos novos. Esses elementos não fazem parte do escopo atual adotado para os diagramas.

## 4. DER lógico: como ler e construir

### 4.1 Notação

Cada retângulo é uma tabela. Dentro dele, escrevam nome, campos, tipos e marcas PK/FK.

| Marca | Significado |
|---|---|
| PK | Chave primária: identifica a linha. |
| FK | Chave estrangeira: referencia outra tabela. |
| NN | NOT NULL: preenchimento obrigatório no banco. |
| UQ | Valor único. Quando houver condição, escrevam a condição em nota. |
| NULL | Campo opcional no banco. Uma regra pode torná-lo obrigatório em certos casos. |
| BIGINT | Inteiro usado nas identificações. |
| INTEGER | Inteiro, como quantidade. |
| BOOLEAN | Verdadeiro ou falso. |
| VARCHAR(n) | Texto com limite de tamanho. |
| TEXT | Texto sem limite fixado pelo tipo. |
| DATE | Data sem horário. |
| TIMESTAMPTZ | Data e hora com informação de fuso. |
| BYTEA | Conteúdo binário, usado na foto. |

IDs implementados são gerados pelo PostgreSQL: BIGINT GENERATED ALWAYS AS IDENTITY. Os limites de texto, tipos SQL e nomes físicos são escolhas técnicas; as informações obrigatórias vêm do ERS.

### 4.2 Tabela usuarios — IMPLEMENTADA

| Campo | Tipo SQL | Marcas / observação |
|---|---|---|
| id | BIGINT | PK, NN, gerado |
| nome | VARCHAR(120) | NN |
| login | VARCHAR(254) | NN, único sem diferenciar maiúsculas/minúsculas |
| senha_hash | TEXT | NN; nunca senha original |
| perfil | VARCHAR(20) | NN: ADMINISTRADOR ou USUARIO |
| ativo | BOOLEAN | NN, padrão TRUE |
| criado_em | TIMESTAMPTZ | NN, automático |
| versao | BIGINT | NN, padrão 1, positiva; controle de concorrência V009 |

Não criem tabelas separadas para administrador e usuário comum. É a mesma tabela com perfis diferentes. Cadastro, edição e desativação estão implementados. Preservar autoria e pelo menos um administrador ativo; não há exclusão física nem reativação pela aplicação.

### 4.3 Tabela ambientes — IMPLEMENTADA

| Campo | Tipo SQL | Marcas / observação |
|---|---|---|
| id | BIGINT | PK, NN, gerado |
| nome | VARCHAR(120) | NN, único sem diferenciar maiúsculas/minúsculas |
| descricao | TEXT | NULL |
| tipo | VARCHAR(10) | NN: SALA ou SETOR |
| ativo | BOOLEAN | NN, padrão TRUE |
| criado_em | TIMESTAMPTZ | NN, automático |
| cor | VARCHAR(12) | NN, padrão TERRACOTA |
| versao | BIGINT | NN, padrão 1, positiva; confirmação de retirada V010 |

Cor aceita TERRACOTA, AZUL, VERDE, VIOLETA ou GRAFITE. É uma melhoria visual aprovada, não status de operação.

Não coloquem sala dentro de setor por uma FK obrigatória. Essa hierarquia não existe no modelo adotado.

### 4.4 Tabela usuario_ambiente — IMPLEMENTADA

| Campo | Tipo SQL | Marcas / referência |
|---|---|---|
| usuario_id | BIGINT | PK composta, FK, NN → usuarios.id |
| ambiente_id | BIGINT | PK composta, FK, NN → ambientes.id |

A chave primária é o par (usuario_id, ambiente_id). Não existe coluna id nessa tabela.

Cada linha significa que uma conta recebeu acesso a um ambiente. O administrador tem acesso geral e não precisa de linhas para todos os ambientes.

### 4.5 Tabela itens — IMPLEMENTADA

| Campo | Tipo SQL | Marcas / referência |
|---|---|---|
| id | BIGINT | PK, NN, gerado |
| ambiente_id | BIGINT | FK, NN → ambientes.id; localização atual |
| nome | VARCHAR(120) | NN |
| categoria | VARCHAR(120) | NN, texto livre |
| forma_registro | VARCHAR(12) | NN: INDIVIDUAL ou QUANTIDADE |
| equipamento | BOOLEAN | NN, padrão FALSE |
| quantidade | INTEGER | NN, maior que zero |
| status | VARCHAR(16) | NN: ATIVO, EM_MANUTENCAO ou INATIVO |
| numero_patrimonio | VARCHAR(120) | NULL no tipo; obrigatório para equipamento, único quando preenchido |
| setor_responsavel_id | BIGINT | FK, NULL no tipo → ambientes.id; obrigatório para equipamento |
| descricao | TEXT | NULL |
| localizacao | VARCHAR(300) | NULL; detalhe como armário ou bancada |
| foto | BYTEA | NULL |
| foto_tipo | VARCHAR(10) | NULL; image/png ou image/jpeg |
| criado_em | TIMESTAMPTZ | NN, automático |
| excluido_em | TIMESTAMPTZ | NULL; data da exclusão lógica, disponível pela API |
| versao | BIGINT | NN, padrão 1, positiva |

Notas ao lado da tabela:

1. INDIVIDUAL exige quantidade 1.
2. QUANTIDADE exige inteiro positivo; o status vale para o conjunto.
3. equipamento=TRUE exige INDIVIDUAL, patrimônio e setor responsável.
4. O responsável deve ser ambiente ativo de tipo SETOR.
5. Sem classificação de equipamento, patrimônio e responsável ficam nulos.
6. Patrimônio não pode ser reutilizado após exclusão lógica; unicidade ignora maiúsculas/minúsculas.
7. Foto e foto_tipo são preenchidos ou ficam nulos juntos.
8. categoria não referencia tabela de categorias no modelo atual.
9. versao detecta formulário desatualizado; não é o número de patrimônio.

### 4.6 Tabela item_historico — IMPLEMENTADA

| Campo | Tipo SQL | Marcas / referência |
|---|---|---|
| id | BIGINT | PK, NN, gerado |
| item_id | BIGINT | FK, NN → itens.id |
| usuario_id | BIGINT | FK, NN → usuarios.id; autor |
| acao | VARCHAR(30) | NN: CADASTRO, EDICAO, ALTERACAO_STATUS ou EXCLUSAO |
| data_hora | TIMESTAMPTZ | NN, automático |
| status_anterior | VARCHAR(16) | NULL |
| status_novo | VARCHAR(16) | NULL |

Os dois campos de status são preenchidos nas alterações que mudam o status. Registros antigos podem não ter esses valores.

O histórico não possui campo para o usuário escrever livremente uma ação. A aplicação o gera automaticamente. Não se edita nem se apaga auditoria pela aplicação. A ação EXCLUSAO é gerada na exclusão lógica, preservando os demais registros.

Não adicionem valorAntigo/valorNovo genéricos como se existissem: a tabela atual registra ação, autor, data e transição de status quando aplicável.

### 4.7 Tabela ordens_servico — IMPLEMENTADA

| Campo | Tipo SQL | Marcas / referência |
|---|---|---|
| id | BIGINT | PK, NN, gerado |
| equipamento_id | BIGINT | FK, NN → itens.id |
| solicitante_id | BIGINT | FK, NN → usuarios.id |
| problema | VARCHAR(2000) | NN |
| prioridade | VARCHAR(5) | NN: BAIXA, MEDIA ou ALTA |
| status | VARCHAR(16) | NN, padrão ABERTA: ABERTA, EM_ANDAMENTO ou CONCLUIDA |
| aberta_em | TIMESTAMPTZ | NN, automático |
| descricao_atendimento | VARCHAR(2000) | NULL no tipo; obrigatório ao concluir |
| fechada_em | TIMESTAMPTZ | NULL antes da conclusão; automático ao concluir |
| versao | BIGINT | NN, padrão 1, positiva |

Notas:

- Só equipamentos individuais recebem OS.
- Aberta → Em Andamento → Concluída, sem salto ou retorno.
- Atendimento e fechamento existem na conclusão; antes, ficam nulos.
- Concluída é imutável.
- A OS não modifica automaticamente o status do equipamento.
- Não há aprovação, recusa ou cancelamento de OS no fluxo definido.
- Não existe ambiente_id na OS: a localização vem do equipamento.
- Não existe restrição de uma única OS aberta por equipamento no ERS adotado.

### 4.8 Tabela manutencoes — IMPLEMENTADA na V007

RF31/RF32/RN13 exigem equipamento, data do serviço, descrição, técnico, autor do lançamento e OS opcional. A tabela abaixo foi implementada na V007, mantendo os nomes da proposta enviada ao grupo. Nomes, limites e tipos são escolhas técnicas, e não exigências literais da empresa.

| Campo | Tipo SQL | Marcas / referência |
|---|---|---|
| id | BIGINT | PK, NN, gerado |
| equipamento_id | BIGINT | FK, NN → itens.id |
| ordem_servico_id | BIGINT | FK, NULL → ordens_servico.id |
| data_realizacao | DATE | NN; data em que o serviço ocorreu |
| descricao_servico | TEXT | NN |
| responsavel_tecnico | VARCHAR(120) | NN; identificação do técnico |
| usuario_id | BIGINT | FK, NN → usuarios.id; autor do lançamento |
| registrado_em | TIMESTAMPTZ | NN, automático; escolha técnica de rastreabilidade |

O técnico não é necessariamente uma conta do sistema. Por isso, esta proposta usa texto para responsavel_tecnico e FK separada para usuario_id.

Uma manutenção pode existir sem OS. Se ligada a uma OS, ambos devem pertencer ao mesmo equipamento. Não inventem obrigação de a OS estar concluída: essa restrição não foi definida no ERS.

Não acrescentem custo, tipo preventiva/corretiva, anexos ou aprovação como se fossem requisitos oficiais.

### 4.9 Tabela movimentacoes — PLANEJADA; desenho físico PROPOSTO

RF33/RN16/RN17 exigem registro de equipamento, origem, destino, data/hora, autor e setor responsável após a operação. A proposta física é:

| Campo proposto | Tipo SQL proposto | Marcas / referência |
|---|---|---|
| id | BIGINT | PK, NN, gerado |
| equipamento_id | BIGINT | FK, NN → itens.id |
| origem_ambiente_id | BIGINT | FK, NN → ambientes.id |
| destino_ambiente_id | BIGINT | FK, NN → ambientes.id |
| setor_responsavel_apos_id | BIGINT | FK, NN → ambientes.id; deve ser SETOR |
| usuario_id | BIGINT | FK, NN → usuarios.id; administrador responsável pela operação |
| data_hora | TIMESTAMPTZ | NN, automático |

Origem é a localização anterior, obtida pelo servidor. Destino é a nova localização. setor_responsavel_apos_id registra a responsabilidade que ficará valendo, não necessariamente o destino.

A operação atualiza ambiente_id e setor_responsavel_id do mesmo item e registra a movimentação em conjunto. Não cria outro equipamento, não muda seu patrimônio e não perde OS/manutenções/históricos.

Somente administrador movimenta. Usuários com acesso ao ambiente atual podem consultar as movimentações do equipamento. OS aberta não impede movimentação, conforme RF33.

### 4.10 Como ligar as tabelas

O pé de galinha indica muitos. Um círculo indica zero/opcional; uma barra indica um. Se ainda houver dúvida sobre o símbolo, acrescentem a multiplicidade por escrito.

Na tabela abaixo, a coluna esquerda é a referência única de cada linha da tabela direita. Exemplo: cada item_historico tem exatamente um usuário; cada usuário pode ter zero ou muitos registros históricos.

| Lado A | Lado B | Quantos B para cada A? | Quantos A para cada B? | Nome da relação |
|---|---|---|---|---|
| usuarios | usuario_ambiente | 0..* | 1 | recebe_atribuicao |
| ambientes | usuario_ambiente | 0..* | 1 | permite_acesso |
| ambientes | itens, por ambiente_id | 0..* | 1 | localiza |
| ambientes | itens, por setor_responsavel_id | 0..* | 0..1 | responde_por |
| itens | item_historico | 0..* | 1 | possui_auditoria |
| usuarios | item_historico | 0..* | 1 | registra_alteracao |
| itens | ordens_servico | 0..* | 1 | possui_os |
| usuarios | ordens_servico | 0..* | 1 | solicita |
| itens | manutencoes | 0..* | 1 | recebe_manutencao |
| ordens_servico | manutencoes | 0..* | 0..1 | relaciona_servico |
| usuarios | manutencoes | 0..* | 1 | registra_manutencao |
| itens | movimentacoes | 0..* | 1 | possui_movimentacao |
| usuarios | movimentacoes | 0..* | 1 | executa_movimentacao |
| ambientes | movimentacoes, por origem_ambiente_id | 0..* | 1 | origem |
| ambientes | movimentacoes, por destino_ambiente_id | 0..* | 1 | destino |
| ambientes | movimentacoes, por setor_responsavel_apos_id | 0..* | 1 | responsabilidade_apos |

As relações de manutenção estão implementadas na V007; as de movimentação continuam planejadas. A cardinalidade 0..1 do responsável do item considera todos os itens; para equipamento, uma nota exige exatamente um setor responsável.

Um item recém-criado já recebe auditoria, então no ciclo normal existe pelo menos um registro. O conector 0..* descreve a relação estrutural permitida pelas FKs; acrescentem a nota da auditoria automática.

Não desenhem linha direta N:N entre usuarios e ambientes adicional às duas linhas da tabela intermediária no DER lógico. A associação já está representada por usuario_ambiente.

Sugestão de disposição: usuarios à esquerda, ambientes no alto/centro, usuario_ambiente entre eles, itens no centro, item_historico abaixo de itens, ordens_servico à direita, manutencoes abaixo de OS e movimentacoes abaixo de ambientes/itens. Rotulem as três linhas de movimentação para ambientes.

## 5. Diagrama de classes de domínio

### 5.1 Formato das caixas

Usem o retângulo UML de classe: nome no topo, atributos no compartimento seguinte. Uma terceira área de operações pode ficar vazia ou ser omitida neste diagrama estrutural.

Não copiem tipos SQL para classes. Usem os tipos abaixo:

| Banco | Tipo usado neste modelo de domínio |
|---|---|
| BIGINT | Long |
| INTEGER | Integer |
| BOOLEAN | Boolean |
| VARCHAR / TEXT | String |
| TIMESTAMPTZ | OffsetDateTime |
| DATE | LocalDate |
| BYTEA | byte[] |
| FK | Associação com outra classe |

Escrevam atributos como nome: Tipo. Podem usar o sinal - para privado. [0..1] indica atributo opcional.

Este é um modelo de domínio baseado nos dados e regras, não uma reprodução literal dos arquivos Java. Os DTOs atuais podem omitir campos ou trazer nomes derivados. Por exemplo, setorResponsavelNome e solicitanteNome são dados obtidos das associações, não atributos independentes que precisam de outra coluna.

### 5.2 Caixas e atributos a escrever

Usuario — domínio correspondente à estrutura implementada:

    - id: Long
    - nome: String
    - login: String
    - senhaHash: String
    - perfil: PerfilUsuario
    - ativo: Boolean
    - criadoEm: OffsetDateTime
    - versao: Long

Ambiente — domínio correspondente à estrutura implementada:

    - id: Long
    - nome: String
    - descricao: String [0..1]
    - tipo: TipoAmbiente
    - ativo: Boolean
    - criadoEm: OffsetDateTime
    - cor: String
    - versao: Long

Item — domínio correspondente à estrutura implementada:

    - id: Long
    - nome: String
    - categoria: String
    - formaRegistro: FormaRegistro
    - equipamento: Boolean
    - quantidade: Integer
    - status: StatusItem
    - numeroPatrimonio: String [0..1]
    - descricao: String [0..1]
    - localizacao: String [0..1]
    - foto: byte[] [0..1]
    - fotoTipo: String [0..1]
    - criadoEm: OffsetDateTime
    - excluidoEm: OffsetDateTime [0..1]
    - versao: Long

As associações representam ambienteAtual: Ambiente e setorResponsavel: Ambiente [0..1]. Escrevam esses nomes nas linhas em vez de repetir IDs como atributos.

ItemHistorico — domínio correspondente à estrutura implementada:

    - id: Long
    - acao: AcaoHistorico
    - dataHora: OffsetDateTime
    - statusAnterior: StatusItem [0..1]
    - statusNovo: StatusItem [0..1]

As associações representam item: Item e usuario: Usuario. Acrescentem a nota {imutável; geração automática}.

OrdemServico — domínio correspondente à estrutura implementada:

    - id: Long
    - problema: String
    - prioridade: PrioridadeOS
    - status: StatusOS
    - abertaEm: OffsetDateTime
    - descricaoAtendimento: String [0..1]
    - fechadaEm: OffsetDateTime [0..1]
    - versao: Long

As associações representam equipamento: Item e solicitante: Usuario. Notas: {somente equipamento individual}, {concluída imutável}, {status independente do item}.

Manutencao — domínio correspondente à V007 implementada:

    - id: Long
    - dataRealizacao: LocalDate
    - descricaoServico: String
    - responsavelTecnico: String
    - registradoEm: OffsetDateTime

Associações: equipamento: Item, ordemServico: OrdemServico [0..1], usuario: Usuario. A distinção entre técnico e autor é a mesma explicada no DER.

Movimentacao — PLANEJADA; atributos técnicos PROPOSTOS:

    - id: Long
    - dataHora: OffsetDateTime

Associações: equipamento: Item, origem: Ambiente, destino: Ambiente, setorResponsavelApos: Ambiente e usuario: Usuario. As informações estão nas associações, portanto a caixa pequena não significa informação faltando.

Não criem subclasse Equipamento para fingir que ela existe no código. Neste modelo simples, Item com equipamento=TRUE representa equipamento individual. Uma especialização conceitual seria outra escolha de modelagem, desnecessária para esta entrega.

### 5.3 Enumerations

Criem caixas pequenas com a marca «enumeration». Elas representam conjuntos de valores; não afirmam que existe um arquivo enum Java ou uma tabela SQL para cada conjunto.

| Nome no diagrama | Valores |
|---|---|
| PerfilUsuario | ADMINISTRADOR, USUARIO |
| TipoAmbiente | SALA, SETOR |
| FormaRegistro | INDIVIDUAL, QUANTIDADE |
| StatusItem | ATIVO, EM_MANUTENCAO, INATIVO |
| AcaoHistorico | CADASTRO, EDICAO, ALTERACAO_STATUS, EXCLUSAO |
| PrioridadeOS | BAIXA, MEDIA, ALTA |
| StatusOS | ABERTA, EM_ANDAMENTO, CONCLUIDA |

Não liguem enumerações com cardinalidade de registros. Basta o atributo usar o tipo ou, se necessário, uma dependência tracejada da classe para a enumeração.

### 5.4 Associações e multiplicidades

Reutilizem os relacionamentos do DER com nomes de classes. A exceção é acesso:

    Usuario 0..* — 0..* Ambiente : possuiAcesso

Nota dessa linha: {no banco, associação implementada por usuario_ambiente; administrador tem acesso geral}.

Para as demais relações, troquem os nomes físicos pelos nomes de classes:

- ambientes → Ambiente
- itens → Item
- usuarios → Usuario
- item_historico → ItemHistorico
- ordens_servico → OrdemServico
- manutencoes → Manutencao
- movimentacoes → Movimentacao

Em toda linha, a multiplicidade escrita perto de uma classe diz quantas instâncias DESSA classe podem se associar a UMA instância da classe da outra ponta.

Exemplo: na linha Item — OrdemServico, escrevam 1 perto de Item e 0..* perto de OrdemServico. Isso significa: uma OS tem um equipamento; um equipamento pode ter várias OS.

Para desenhar sem precisar converter nomes, copiem estas linhas:

| Perto da classe A | Perto da classe B | Nome da associação |
|---|---|---|
| Usuario 0..* | Ambiente 0..* | possuiAcesso |
| Ambiente 1 | Item 0..* | ambienteAtual |
| Ambiente 0..1 | Item 0..* | setorResponsavel |
| Item 1 | ItemHistorico 0..* | historico |
| Usuario 1 | ItemHistorico 0..* | autor |
| Item 1 | OrdemServico 0..* | equipamento |
| Usuario 1 | OrdemServico 0..* | solicitante |
| Item 1 | Manutencao 0..* | equipamento |
| OrdemServico 0..1 | Manutencao 0..* | ordemServico |
| Usuario 1 | Manutencao 0..* | autorDoLancamento |
| Item 1 | Movimentacao 0..* | equipamento |
| Usuario 1 | Movimentacao 0..* | responsavelPelaOperacao |
| Ambiente 1 | Movimentacao 0..* | origem |
| Ambiente 1 | Movimentacao 0..* | destino |
| Ambiente 1 | Movimentacao 0..* | setorResponsavelApos |

As três últimas linhas são três associações diferentes entre as mesmas classes. Não as substituam por uma linha sem identificação.

Usem associações simples, sem losangos de composição. Preservação de histórico não deve sugerir apagar registros em cascata ao retirar um item.

Não coloquem métodos login(), salvarNoBanco() ou gerenciarUsuarios() dentro de Usuario só porque essas ações usam uma conta. O código atual separa responsabilidades em controller, serviço e DAO. Se o professor exigir classes de implementação e métodos, será necessário um segundo diagrama com esse outro escopo.

## 6. Diagrama de casos de uso

### 6.1 Desenho básico

1. Façam um retângulo grande: STOCKA.
2. Fora dele, desenhem os atores. Atores são pessoas/perfis externos; não são tabelas.
3. Dentro, desenhem elipses com as ações abaixo.
4. Liguem atores a ações por linhas simples.

Para evitar repetir todas as linhas, proponho três atores:

- Usuario autenticado: ator geral, que concentra ações compartilhadas.
- Usuario comum: especialização do ator geral.
- Administrador: especialização do ator geral.

Desenhem uma generalização saindo de Usuario comum para Usuario autenticado e outra saindo de Administrador para Usuario autenticado: linha contínua, triângulo vazio apontando para o ator geral.

O ator geral não é um terceiro perfil no banco. Os únicos perfis continuam sendo ADMINISTRADOR e USUARIO.

Login deve ligar diretamente aos dois atores concretos, porque acontece antes da autenticação. Ações administrativas ligam somente ao Administrador. Ações compartilhadas ligam ao Usuario autenticado, com a nota {usuário comum: somente ambientes atribuídos; administrador: acesso geral}.

### 6.2 Nomes das elipses e permissões

Esta tabela agrupa requisitos que descrevem a mesma ação; não é necessário inventar uma elipse por cada campo ou validação.

| Nome da elipse | RF | Quem participa | Situação |
|---|---|---|---|
| Autenticar | RF01 | Usuario comum e Administrador | Implementado |
| Cadastrar usuario | RF02 | Administrador | Implementado |
| Alterar propria senha | RF03 | Usuario autenticado | Implementado |
| Gerenciar dados e perfil de usuario | RF04 | Administrador | Implementado |
| Desativar usuario | RF07 | Administrador | Implementado |
| Cadastrar ambiente | RF08/RF09 | Administrador | Implementado |
| Atribuir acesso a ambientes | RF10/RF11 | Administrador | Implementado |
| Editar ambiente | RF12 | Administrador | Implementado |
| Retirar ambiente de uso | RF13 | Administrador | Implementado por desativação; bloqueado enquanto houver vínculos |
| Consultar ambientes | RF11/RF14/RF15 | Usuario autenticado, conforme perfil | Implementado para ambientes ativos |
| Pesquisar ambientes | RF16 | Usuario autenticado | Implementado |
| Cadastrar item | RF17/RF18/RF25 | Usuario autenticado e autorizado | Implementado |
| Editar item | RF19 | Usuario autenticado e autorizado | Implementado com limites atuais de edição |
| Excluir item logicamente | RF20 | Usuario autenticado e autorizado | Implementado |
| Consultar itens excluidos | RF20/RN15 | Administrador | Implementado |
| Alterar status do item | RF21 | Usuario autenticado e autorizado | Implementado |
| Registrar auditoria do item | RF22 | Comportamento automático, sem ator humano direto | Implementado |
| Consultar historico de alteracoes | RF23 | Usuario autenticado e autorizado | Implementado |
| Cadastrar multiplos itens | RF24 | Usuario autenticado e autorizado | Planejado |
| Pesquisar itens | RF26 | Usuario autenticado e autorizado | Implementado |
| Acessar inventario | RF06/RF27 | Usuario autenticado; administrador com acesso geral | Implementado |
| Abrir OS | RF28 | Usuario autenticado e autorizado | Implementado |
| Consultar OS | RF29 | Usuario autenticado e autorizado | Implementado |
| Atualizar e concluir OS | RF30 | Usuario autenticado e autorizado | Implementado |
| Registrar manutencao | RF31 | Usuario autenticado e autorizado | Implementado |
| Consultar historico de manutencao | RF32 | Usuario autenticado e autorizado | Implementado |
| Movimentar equipamento | RF33 | Administrador | Planejado |
| Consultar movimentacoes | RF33 | Usuario autenticado e autorizado | Planejado |

RF05 e RF06 são regras transversais de perfil/autorização: anotem que o sistema identifica o perfil e valida acesso na API em todas as operações protegidas. Não precisam virar ações clicáveis artificiais.

Na elipse Consultar ambientes, o administrador vê todos os ambientes ativos e o usuário comum apenas os atribuídos. Não liguem ambos ao RF15 como se o usuário comum tivesse visão geral irrestrita.

### 6.3 include e extend

Para este desenho, usem include somente para auditoria obrigatória:

- Cadastrar item → Registrar auditoria do item.
- Editar item → Registrar auditoria do item.
- Alterar status do item → Registrar auditoria do item.
- Excluir item logicamente → Registrar auditoria do item.

A linha é tracejada e a seta aponta para Registrar auditoria do item. Escrevam «include» sobre ela. A seta sai da ação principal e aponta para o comportamento obrigatório incluído.

Não liguem um ator humano diretamente a Registrar auditoria: não existe botão para escrever esse histórico.

Não é necessário usar extend neste diagrama. Não usem include/extend para significar apenas “essa tela fica dentro de outra” ou “isso acontece depois”.

Em notas, registrem:

- desativação de conta preserva autoria e protege o último administrador;
- retirada de ambiente verifica dependências e preserva referências;
- exclusão de item é lógica e bloqueada com OS Aberta/Em Andamento;
- OS/manutenção pertencem somente a equipamentos individuais;
- movimentação é administrativa, mas sua consulta é autorizada pelo ambiente atual.

## 7. Diagrama de sequência RF28 — Abrir OS

### 7.1 Participantes

Desenhem da esquerda para a direita:

1. Usuario autorizado — ator.
2. Interface Vue — tela/componente OrdensServico.
3. API Java — OrdemServicoController.
4. Acesso a dados — OrdemServicoDAO.
5. PostgreSQL.

Cada participante tem uma linha de vida vertical tracejada. As mensagens são lidas de cima para baixo. Mensagem enviada usa linha contínua; retorno usa tracejada.

É uma sequência de integração. As chamadas internas de SessaoService, AutenticacaoService e UsuarioDAO ficam resumidas na validação da API; não é uma representação de cada método do código.

Pré-condições: conta autenticada, equipamento já selecionado e ambiente ativo.

### 7.2 Mensagens do caminho de sucesso

| Ordem | De → Para | Texto da seta |
|---|---|---|
| 1 | Usuario → Vue | Solicitar abertura de OS |
| 2 | Vue → Usuario | Apresentar problema, prioridade e solicitante |
| 3 | Usuario → Vue | Informar problema e prioridade; confirmar |
| 4 | Vue → API | POST /api/ambientes/{ambienteId}/itens/{itemId}/ordens-servico |
| 5 | API → API | Validar sessao e conta ativa; obter usuarioId |
| 6 | API → API | Validar problema e prioridade |
| 7 | API → DAO | criar(usuarioId, ambienteId, itemId, dados) |
| 8 | DAO → PostgreSQL | BEGIN |
| 9 | DAO → PostgreSQL | Verificar acesso ao ambiente atual e equipamento individual nao excluido |
| 10 | PostgreSQL → DAO | Equipamento e acesso validos |
| 11 | DAO → PostgreSQL | INSERT ordem com equipamento, solicitante, problema e prioridade |
| 12 | PostgreSQL → PostgreSQL | Aplicar status ABERTA, abertura automatica e validar vinculo |
| 13 | PostgreSQL → DAO | Identificacao da OS criada |
| 14 | DAO → PostgreSQL | Consultar dados da OS e COMMIT |
| 15 | DAO → API | OrdemServico criada |
| 16 | API → Vue | 201 Created, OS e Location |
| 17 | Vue → Usuario | Informar sucesso e mostrar OS |

Os dados do POST são problema e prioridade. Cookie de sessão identifica a conta; o frontend não decide solicitante, data ou status inicial. Não coloquem INSERT no frontend.

Mensagens como “consultar dados e COMMIT” podem ser duas setas se desejarem mais detalhe.

### 7.3 Alternativas

Criem fragmentos alt nos locais correspondentes. São saídas alternativas, não passos que sempre acontecem antes do sucesso:

- [sessao ausente/expirada]: API retorna 401 e a interface pede novo login; sem INSERT.
- [dados invalidos]: API retorna 400 e o formulário mantém o rascunho; sem INSERT.
- [sem acesso ou equipamento indisponivel]: DAO encerra a transação sem inserir; API retorna 404.
- [item nao e equipamento]: retorno 400, sem inserir.
- [falha durante gravacao]: ROLLBACK; interface apresenta falha, sem informar sucesso.

Desenhem a ativação do PostgreSQL envolvendo a transação. Não desenhem alteração de status do equipamento nem cadastro de manutenção: nenhuma dessas ações faz parte de abrir OS.

## 8. Diagrama de sequência RF30 — Concluir OS

### 8.1 Participantes e pré-condições

Mesmos cinco participantes. A interface agora usa OrdemServicoEdicao.

Pré-condições: usuário autenticado e autorizado; OS existente em EM_ANDAMENTO; interface possui a versão lida.

O avanço ABERTA → EM_ANDAMENTO usa a mesma rota PUT, mas não exige atendimento nem gera fechamento. Uma nota pode explicar isso sem criar um terceiro diagrama.

### 8.2 Caminho de sucesso

| Ordem | De → Para | Texto da seta |
|---|---|---|
| 1 | Usuario → Vue | Selecionar Atualizar OS |
| 2 | Vue → Usuario | Apresentar dados e fases permitidas |
| 3 | Usuario → Vue | Selecionar Concluida, descrever atendimento e confirmar |
| 4 | Vue → API | PUT /api/ambientes/{ambienteId}/itens/{itemId}/ordens-servico/{ordemId} |
| 5 | API → API | Validar sessao, conta ativa e presenca da versao |
| 6 | API → DAO | atualizar(usuarioId, ambienteId, itemId, ordemId, dados) |
| 7 | DAO → PostgreSQL | BEGIN; verificar autorizacao e equipamento |
| 8 | DAO → PostgreSQL | Consultar OS com FOR UPDATE |
| 9 | PostgreSQL → DAO | Dados e versao atuais |
| 10 | DAO → DAO | Comparar versao; verificar OS nao concluida |
| 11 | DAO → DAO | Validar transicao e descricao do atendimento |
| 12 | DAO → PostgreSQL | UPDATE problema, prioridade, status e atendimento |
| 13 | PostgreSQL → PostgreSQL | Validar regras; gerar fechada_em e incrementar versao |
| 14 | PostgreSQL → DAO | Atualizacao aceita |
| 15 | DAO → PostgreSQL | Consultar OS atualizada e COMMIT |
| 16 | DAO → API | OrdemServico concluida |
| 17 | API → Vue | 200 OK e dados atualizados |
| 18 | Vue → Usuario | Mostrar fechamento e atendimento; bloquear edicao da concluida |

O PUT envia problema, prioridade, status, descricaoAtendimento e versao. Não envia data de fechamento escolhida pelo usuário. O servidor preserva solicitante, equipamento e abertura originais.

### 8.3 Alternativas

- [sessao ausente/expirada]: 401, sem atualização.
- [sem acesso ou OS nao pertence ao equipamento]: 404, sem atualização.
- [versao desatualizada]: ROLLBACK e 409 OS_ALTERADA, com ordemAtual.
- [OS ja concluida]: ROLLBACK e 409 OS_CONCLUIDA, com ordemAtual.
- [salto de fase, retorno ou atendimento vazio]: ROLLBACK e 400.
- [erro de gravacao]: ROLLBACK e mensagem de falha.

No ramo 409, acrescentem a reação do frontend: preservar rascunho e desabilitar envio; oferecer Carregar versão atual ou Consultar OS concluída. Carregar a versão atual é uma escolha explícita que substitui o formulário.

Uma nota deve dizer: {nenhum UPDATE em itens; status do equipamento independente}. Concluir OS também não gera automaticamente um registro de manutenção.

O passo 3 textual do RF30 ainda menciona informar data, mas a descrição do requisito e RN11 determinam fechamento automático. A implementação e este diagrama seguem a regra automática; o texto do documento precisa desse alinhamento.

## 9. Revisão antes da entrega

Confiram uma figura por vez:

1. O título informa o tipo do diagrama e seu escopo?
2. Toda parte futura está marcada PLANEJADA ou PROPOSTA TÉCNICA?
3. Os nomes de item, ambiente, OS e manutenção são consistentes entre figuras?
4. Sala e setor estão classificados no mesmo conceito Ambiente, sem hierarquia inventada?
5. Localização e responsabilidade estão desenhadas separadamente?
6. Acesso vem das atribuições ao ambiente atual, e não do setor responsável?
7. Usuário comum pode realizar as ações autorizadas do inventário e OS?
8. Apenas administrador gerencia contas/ambientes/acessos e movimenta equipamento?
9. Equipamento tem patrimônio e quantidade 1?
10. Lotes não recebem OS ou manutenção por unidade?
11. OS usa somente ABERTA, EM_ANDAMENTO, CONCLUIDA?
12. Encerramento exige atendimento e gera data automaticamente?
13. Concluída não tem caminho de retorno ou edição?
14. Auditoria, OS e manutenção são conceitos separados?
15. O DER mostra todas as PKs/FKs e as cardinalidades nas pontas certas?
16. A sequência inclui alternativa de erro e não informa sucesso antes de COMMIT?
17. Nenhum desenho sugere token guardado no navegador ou senha original no banco?
18. O arquivo .drawio foi salvo e a exportação está legível?

## 10. Divisão sugerida do trabalho

| Entrega | Fonte principal | O que conferir com os demais |
|---|---|---|
| DER lógico | Migrações V001–V006 e propostas deste guia | Campos, PKs/FKs, cardinalidades e indicação de planejamento |
| Classes de domínio | Classes e associações da seção 5 | Mesmos conceitos e restrições do DER, sem confundir tabela com classe |
| Casos de uso | ERS e seção 6 | Perfis, permissões e cobertura dos RF |
| Sequências RF28/RF30 | API/DAO e seções 7–8 | Ordem das validações, transações, retornos e independência do equipamento |
| Revisão conjunta | Todas as figuras e ERS | Consistência de nomes, regras e situação de implementação |

Não é preciso haver cinco pessoas: uma pessoa pode assumir mais de uma entrega. O importante é revisar em conjunto os relacionamentos compartilhados antes de inserir as figuras no Word.

## 11. Fontes internas para conferir

- ERS_STOCKA_Sprint2.docx: requisitos do projeto, ainda fora do repositório.
- docs/decisoes-e-proximos-passos.md: decisões, limites e pendências.
- docs/modelo-banco.md: explicação da estrutura.
- docs/ordens-servico.md: comportamento e contratos das OS.
- database/migrations/V001__acesso_ambientes.sql.
- database/migrations/V002__cor_ambiente.sql.
- database/migrations/V003__inventario.sql.
- database/migrations/V004__edicao_auditoria_itens.sql.
- database/migrations/V005__abertura_ordens_servico.sql.
- database/migrations/V006__andamento_encerramento_os.sql.
- database/migrations/V007__manutencoes.sql.
- database/migrations/V008__exclusao_logica_itens.sql.

Se houver diferença entre este guia e uma nova versão aprovada do ERS ou novas migrações, revisem o guia antes de continuar o desenho. O modelo físico de movimentação continua uma proposta, ainda sem SQL implementado; manutenção já acompanha V007.
