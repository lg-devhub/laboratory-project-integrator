# Edição de itens e dados de demonstração

## O que está disponível

No inventário, abra os detalhes do item e clique em **Editar item**. Administradores
e usuários atribuídos ao ambiente atual podem editar nome, categoria, quantidade
(nos registros por quantidade), descrição, localização detalhada, foto e status.
A API verifica novamente as permissões; esconder botões não substitui essa verificação.

É possível manter, substituir ou remover a foto. Cancelar não grava alterações.
Uma gravação sem mudança efetiva não cria outra entrada de auditoria.

Patrimônio, forma de registro, classificação como equipamento, ambiente atual e
setor responsável ficam preservados nesta edição. Essa é uma decisão técnica de
escopo deste incremento, não uma proibição adicional definida pela empresa.
Movimentação administrativa e eventuais correções desses vínculos precisam de um
fluxo próprio; não foram implementadas por este formulário.

## Duas pessoas editando o mesmo item

Cada item tem uma versão. Se outra pessoa salvar primeiro, a segunda gravação
recebe conflito (HTTP 409), preserva o rascunho na tela e bloqueia nova submissão.
O botão **Carregar versão atual** substitui explicitamente os campos pelos dados
atuais. Confira as alterações e reaplique o que ainda fizer sentido antes de salvar.
O sistema não sobrescreve silenciosamente o trabalho da outra pessoa.

V004 acrescenta `itens.versao` e os campos opcionais `status_anterior/status_novo`
ao histórico. Os triggers registram autor da sessão, data do servidor e ação.
Uma mudança apenas de status usa ALTERACAO_STATUS; mudanças em outros campos usam
EDICAO, incluindo a transição de status quando houver. Histórico antigo é preservado.
Falha ao registrar auditoria desfaz a alteração inteira, inclusive a versão.

Esses comportamentos atendem ao incremento de RF19/RF21/RF22 e RNF08. O histórico
de alterações não substitui o futuro histórico de manutenções.

## Popular o banco de trabalho

Com banco e API atualizados e ativos, execute na raiz:

```powershell
.\Popular-Banco.ps1
```

O script usa a conta local de demonstração `admin@stocka.local`, com a senha inicial
fictícia definida no seed. Se você a alterou, informe a nova senha sem gravá-la:

```powershell
$senhaDemo = Read-Host 'Senha da conta administradora local' -AsSecureString
.\Popular-Banco.ps1 -Senha $senhaDemo
```

O script aceita somente API HTTP local e exige que a configuração DB_URL deste
checkout aponte para `stocka` em localhost/127.0.0.1:15432. Essa verificação da
configuração não atesta a conexão usada por uma API iniciada de outro checkout.
Não use este script contra dados de produção.

Os exemplos estão em `database/seed/demonstracao.json`:

| Ambiente | Tipo | Itens |
|---|---|---|
| DEMO - Produção | SETOR | 3 |
| DEMO - Manutenção | SETOR | 3 |
| DEMO - Sala de ferramentas | SALA | 3 |
| DEMO - Sala de inspeção | SALA | 3 |

São 12 registros: oito equipamentos com patrimônio DEMO-PAT-001 a DEMO-PAT-008,
três registros por quantidade e um item individual sem patrimônio. Os exemplos
incluem status diferentes e equipamentos em salas com responsabilidade de um setor.
Não recebem fotos fictícias. Nomes e descrições identificam a demonstração.

Cadastros são feitos pela API e geram auditoria real. O operador local existente,
se estiver ativo, recebe acesso somente aos quatro ambientes DEMO. Atribuições
anteriores são mantidas. Não são criadas contas nem alterados cadastros anteriores.
Reexecutar o mesmo arquivo não duplica os exemplos existentes; isso não é uma
ferramenta de sincronização para sobrescrever edições feitas nos exemplos.

## Organização e verificação

Foram retirados o favicon.ico sem uso, a pasta vazia assets/images com seu .gitkeep
e um relatório de falha da JVM preservado no backup externo desta revisão.
node_modules, target e dist são arquivos gerados e continuam ignorados pelo Git.

Os scripts Java usam limites de memória para Maven e JVM, com verificações executadas
em sequência. O limite de heap não corresponde ao consumo total do processo.
Iniciar-API.ps1 permite ajustar os argumentos pelo parâmetro ArgumentosJvm.

Testar-Backend.ps1 executa 35 testes de autenticação/ambientes, 19 de inventário e quatro de alteração de senha
em banco temporário exclusivo. A conferência em navegador incluiu cadastro, edição,
troca de status, foto mantida/removida, duas abas em conflito, rascunho preservado,
recarga explícita, cancelamento e larguras de 1440, 390 e 320 pixels.

Exclusão lógica, retirada de ambientes, movimentações, OS, manutenção, gestão de
contas e cadastro de vários registros continuam pendentes. A carga fictícia é
um recurso de demonstração autorizado, não um novo requisito da empresa.

## Conferência da carga em 24/10/2026

A execução criou quatro ambientes e 12 itens. A repetição criou zero ambientes e
zero itens. A comparação dos registros anteriores confirmou a preservação das
quatro contas, três ambientes, três atribuições, um item e sua entrada de histórico.
O banco passou a ter sete ambientes, 13 itens e 13 entradas de histórico.
