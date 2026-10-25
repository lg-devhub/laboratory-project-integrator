# Alteração da própria senha

No hub, clique em **Alterar senha**. Informe a senha atual, a nova senha e sua
confirmação. Administrador e Usuário podem usar o mesmo formulário, inclusive
contas sem ambientes atribuídos. Cancelar não grava nada.

RF03 exige alteração da própria senha; RN06 define pelo menos oito caracteres,
uma letra e um número. Pedir a senha atual e confirmar a nova são decisões de
implementação. O limite técnico de 72 bytes em UTF-8 é o mesmo do login BCrypt;
caracteres acentuados podem ocupar mais de um byte. A senha não é aparada nem
convertida para maiúsculas/minúsculas.

PUT /api/auth/senha recebe senhaAtual, novaSenha e confirmacao. A identidade vem
exclusivamente da sessão. A API valida os campos, confere a senha atual e grava
somente o novo hash BCrypt na conta ativa autenticada. Nunca retorna senha/hash.
Se o hash tiver mudado durante a operação, a gravação é recusada.

Sucesso retorna 204 e renova o identificador da sessão atual, mantendo a pessoa
conectada. Falta de sessão retorna 401; dados inválidos/senha atual incorreta
retornam 400. O cabeçalho de escrita e a verificação de origem continuam obrigatórios.
Outras sessões já abertas não são encerradas por este incremento. Não há recuperação
por e-mail nem alteração de senha de outra conta por administrador neste formulário.

Arquivos envolvidos: AlterarSenha.vue, services/autenticacao.ts e AmbientesView.vue
no frontend; AutenticacaoController, AutenticacaoService e UsuarioDAO no backend.
Não foi necessária mudança de esquema do banco.

Testar-Backend.ps1 executa 78 verificações: 35 existentes, 19 de inventário, quatro de senha, 12 de OS e oito de manutenção/resumo. Os testes de senha usam somente contas fictícias novas no banco temporário
de Testar-Inventario.ps1. Nenhuma senha do banco de trabalho é alterada pelos testes.

Os testes antigos de login ainda pressupõem as credenciais iniciais do seed local.
Se você trocar a senha de uma dessas contas no banco de trabalho, essa parte dos
testes precisará ser adaptada para usar dados de teste independentes; não restaure
a senha real apenas para satisfazê-los. Os testes novos já usam banco descartável.
