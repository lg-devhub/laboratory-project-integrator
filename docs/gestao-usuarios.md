# Gestão de usuários

## Requisitos e decisões

RF02 permite ao administrador cadastrar nome, login, senha e perfil; RF04 cobre
edição administrativa; RF07/RN14 exigem desativação com preservação de autoria e
do último administrador ativo. RN06 exige pelo menos oito caracteres, uma letra
e um número. O limite de 72 bytes UTF-8 é técnico, devido ao BCrypt utilizado.

Impedir também o rebaixamento do último administrador é uma interpretação técnica
necessária para preservar a mesma regra. Controle de versão, pesquisa local e
organização em uma tela própria são decisões de implementação.

## Uso

No hub, o administrador abre **Gerenciar usuários**. A tela lista contas ativas e
desativadas, permite buscar por nome/login e oferece:

- Cadastro: conta inicialmente ativa, perfil Usuário ou Administrador e senha com confirmação.
- Edição: nome, login e perfil; nova senha opcional. Campo vazio mantém a senha atual.
- Desativação: confirmação explícita, sem apagar a conta, atribuições ou referências de autoria.

Não existe reativação neste incremento. Editar uma conta desativada mantém a
desativação. Seu login continua reservado, sem distinção entre maiúsculas e minúsculas.
Atribuições aos ambientes continuam na opção **Acessos** do hub.

Alterações de perfil/situação são verificadas pela API a cada requisição. Se o
administrador editar seu próprio perfil para Usuário, retorna ao hub; se desativar
a própria conta, retorna ao login. Ambas as operações exigem outro administrador
ativo. Alteração administrativa de senha muda os próximos logins; não encerra
automaticamente outras sessões já autenticadas. Desativação bloqueia essas sessões
na próxima requisição autenticada.

## Contrato da API

Todas as rotas exigem administrador ativo. Escritas exigem cookie de sessão,
Content-Type application/json e X-STOCKA-Request: true.

| Método e rota | Entrada | Resultado |
|---|---|---|
| GET /api/usuarios | — | Lista id, nome, login, perfil, ativo e versao. |
| POST /api/usuarios | nome, login, perfil, senha | 201 com conta pública; ativa e versão 1. |
| PUT /api/usuarios/{id} | nome, login, perfil, senha opcional, versao | 200 com conta atualizada. |
| POST /api/usuarios/{id}/desativacao | versao | 200 com conta desativada. |

Nenhuma resposta inclui senha ou hash. 400 indica entrada inválida; 401, sessão
ausente/inativa; 403, falta de perfil administrativo; 404, conta inexistente.
409 informa login reservado, ULTIMO_ADMINISTRADOR ou USUARIO_ALTERADO. Neste último
caso, usuarioAtual contém somente dados públicos. O formulário preserva o rascunho
e exige carregar explicitamente a versão atual antes de tentar salvar novamente.
Essa recarga substitui os campos e limpa a senha digitada.

## Banco e arquivos

V009 acrescenta usuarios.versao BIGINT, inicialmente 1. Alterações efetivas de
nome/login/senha/perfil/ativo incrementam a versão, inclusive pela troca da própria
senha. A API exige a versão anterior para edição/desativação. V009 não altera
senhas, perfis nem situações das contas existentes.

As escritas administrativas adquirem um bloqueio de tabela antes de conferir
novamente o administrador e a conta alterada. Isso serializa essas operações no
pequeno cadastro de contas e protege contra duas desativações simultâneas.
O trigger também impede retirar o último administrador ativo por atualização SQL.
Não há rota de exclusão física de usuários.

UsuarioController autoriza as rotas; GestaoUsuarioDAO realiza a transação;
UsuarioDados valida a entrada e UsuarioGestao representa a resposta pública.
UsuariosView apresenta a tela; UsuarioFormulario e UsuarioDesativacao cuidam dos
diálogos; services/usuarios.ts concentra as requisições.

## Verificação

GestaoUsuarioLocalTest tem oito testes de integração em banco temporário:
permissões e respostas sem hash, validação/unicidade, edição e senha opcional,
desativação e sessão existente, último administrador, conflito de versão,
edição sem reativação e desativações administrativas concorrentes.

Testar-Inventario.ps1 inclui essa suíte, totalizando 67 testes no banco descartável.
Testar-Backend.ps1 executa também os 35 testes anteriores de autenticação/ambientes:
102 verificações disponíveis no conjunto. Não use a quantidade como medida de
conclusão do ERS; movimentação e RF24 continuam pendentes. A retirada de ambientes
está disponível na V010, conforme o guia específico.

Em 25/10/2026, os 59 testes do banco temporário passaram, assim como a compilação
Vue/TypeScript. A conferência no navegador cobriu cadastro, confirmação de senha,
edição com/sem nova senha, conflito e recarga explícita, cancelamento/desativação,
último administrador, perda de perfil e bloqueio da sessão de conta desativada.
As larguras 1440, 390 e 320 pixels foram verificadas, sem erros JavaScript.

V009 foi aplicada no banco local. A comparação de todas as linhas das sete tabelas
antes/depois confirmou preservação dos dados anteriores, desconsiderando apenas
o novo campo usuarios.versao. Os bancos temporários foram removidos.
