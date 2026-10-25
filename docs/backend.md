# Backend STOCKA

Java 21+, Spring Boot 4.0.8 e PostgreSQL via JDBC. O backend autentica contas, administra sessões, consulta ambientes autorizados e permite ao administrador cadastrar/editar ambientes e gerenciar atribuições.
Login e hub Vue já estão integrados à API.

## Executar e testar

Com Docker Desktop iniciado, execute na raiz do projeto:

```powershell
.\Iniciar-Banco.ps1
.\Iniciar-API.ps1
```

A API atende em http://localhost:8080 e continua executando nesse terminal.
Ctrl+C encerra a API. PostgreSQL e pgAdmin executam no Docker; o Java executa
separadamente no Windows. Abrir a raiz da API no navegador não abre uma tela.

Iniciar-API.ps1 verifica a porta antes de executar Maven e avisa quando já existe
um serviço atendendo; não tenta iniciar uma segunda instância. Esse aviso não
confirma que qualquer serviço na porta seja o STOCKA.

Para parar uma API iniciada em segundo plano, execute .\Parar-API.ps1 na raiz.
O script verifica a porta, a classe Java e o processo Maven da pasta deste projeto
antes de encerrar. Não encerra outras aplicações Java nem para Docker/Vue. Para
reiniciar, execute Parar-API.ps1 e depois Iniciar-API.ps1. As sessões anteriores
expiram ao reiniciar; entre novamente no site. Se a API foi iniciada por uma IDE,
use o botão de parar da própria IDE quando o script não reconhecer a origem.

Para testar, execute na raiz:

```powershell
.\Testar-Backend.ps1
```

O comando testa tanto a validação das credenciais quanto as rotas HTTP. Ele inicia
uma API temporária em porta livre e a encerra ao terminar. Apenas o banco precisa
estar iniciado previamente. O Maven Wrapper baixa o Maven e as dependências
na primeira execução; não é necessário instalar o Maven manualmente.

No Linux/macOS, dentro de backend, os comandos correspondentes são
`sh mvnw spring-boot:run` e `sh mvnw test -Dstocka.testesLocais=true`.
No Windows, dentro de backend, use `mvnw.cmd` com os mesmos argumentos.

## Configuração do banco

O backend/.env contém DB_URL, DB_USER e DB_PASSWORD. Neste computador, o arquivo
já foi preparado para o banco local. Em outra máquina:

1. Inicie o banco pelo script da raiz, que gera o .env da raiz se necessário.
2. Copie backend/.env.example para backend/.env.
3. Preencha DB_PASSWORD com o valor de POSTGRES_PASSWORD do .env da raiz.

A URL local é `jdbc:postgresql://localhost:15432/stocka?connectTimeout=5&socketTimeout=10`
e o usuário PostgreSQL é stocka. Essa senha é a de conexão ao banco, não a senha
de uma conta da aplicação.

O .env da raiz configura Docker, PostgreSQL e pgAdmin. O backend/.env configura
somente o acesso do Java ao banco. Alterar um não sincroniza o outro nem muda a
senha de um banco já criado. Ambos ficam fora do Git; mantenha os .env.example.
Variáveis do sistema têm prioridade sobre as configurações lidas por dotenv.
Execute pelos comandos documentados para que o diretório de leitura seja backend.

## Caminho de uma requisição

1. ApiApplication inicia o servidor HTTP.
2. ApiFiltro verifica a origem e os cabeçalhos da requisição.
3. AutenticacaoController recebe os dados da rota.
4. AutenticacaoService aplica a validação das credenciais ou da conta ativa.
5. UsuarioDAO consulta PostgreSQL usando uma conexão aberta por Connect.
6. O controlador devolve JSON ou um status HTTP; ApiErros trata as falhas previstas.

O DAO usa parâmetros SQL para os valores recebidos. A conexão, o comando e o
resultado são fechados automaticamente com try-with-resources.

## Arquivos que compõem a aplicação

Os caminhos Java abaixo são relativos a backend/src/main/java/br/com/projetointegrador.

| Arquivo | Responsabilidade |
|---|---|
| ApiApplication.java | Iniciar o Spring Boot e o servidor HTTP. |
| config/ApiFiltro.java | CORS, cabeçalho de proteção e cache das respostas. |
| controller/AutenticacaoController.java | Login, consulta da conta e logout. |
| controller/ApiErros.java | Traduzir falhas previstas em respostas HTTP. |
| service/AutenticacaoService.java | Validar credenciais e consultar conta ativa. |
| dao/UsuarioDAO.java | Buscar usuário por login ou id. |
| database/Connect.java | Ler a configuração e abrir a conexão JDBC. |
| model/Usuario.java | Dados internos do usuário, incluindo situação ativa e hash. |
| model/UsuarioAutenticado.java | Dados de resposta: id, nome, login e perfil. |

UsuarioAutenticado é um record: uma classe compacta de dados imutáveis. Não contém
senha nem hash. Usuario é de uso interno e não deve ser retornado pela API.

application.properties, em src/main/resources, configura o servidor e a sessão.
pom.xml declara dependências e compilação. mvnw, mvnw.cmd e .mvn/wrapper permitem
executar o Maven sem instalação manual. ApiApplication é o único ponto de entrada
da aplicação; a antiga verificação isolada da conexão foi retirada.

## Contrato HTTP

| Método e caminho | Entrada | Resultado |
|---|---|---|
| POST /api/auth/login | JSON com login e senha | 200 com id, nome, login e perfil; cria sessão. |
| GET /api/auth/me | Cookie de sessão | 200 com os dados atuais da conta; 401 sem acesso válido. |
| POST /api/auth/logout | Cookie, se houver | 204 sem corpo; invalida a sessão. |

Exemplo de entrada com dados fictícios locais:

```json
{"login":"admin@stocka.local","senha":"Stocka123"}
```

Todos os POST e PUT exigem `X-STOCKA-Request: true`. O login também exige
`Content-Type: application/json`. A falta do cabeçalho de proteção retorna 403.
O navegador deve usar `credentials: 'include'` no fetch para receber e reenviar o
cookie. A origem permitida para o Vue é http://localhost:5173.
O cabeçalho combinado com CORS restrito protege contra envios de outras origens;
ele não é um segredo nem substitui a autenticação.

Login inexistente, senha incorreta e conta desativada retornam igualmente 401
com `{"mensagem":"Login ou senha invalidos."}`. JSON inválido ou campos obrigatórios
ausentes retornam 400. Uma origem não permitida retorna 403 pelo processador CORS.
Falhas SQL retornam 503; erros internos de configuração retornam 500. As respostas
não incluem senha, hash, URL de conexão ou detalhes internos do banco.

A base Vue já possui os serviços deste contrato e a consulta da sessão pelo router.
O formulário de login e a saída já estão integrados. O hub consulta os ambientes permitidos em GET /api/ambientes.

## Credenciais, perfis e sessão

AutenticacaoService busca a conta, verifica se está ativa e compara a senha com
senha_hash usando BCrypt. Optional representa um resultado presente ou ausente:
credenciais recusadas retornam Optional vazio; falhas SQL são propagadas separadamente.

Conta ativa sem atribuições também pode entrar. O hub deverá apresentar ausência
de ambientes, sem conceder acessos adicionais. O perfil não é escolhido pela tela.
As permissões de ambientes são aplicadas na consulta SQL do backend.

O login segue a unicidade sem diferenciar maiúsculas adotada no banco. Espaços
externos do login são removidos, mas a senha é comparada sem modificações.
Campos obrigatórios ausentes ou vazios são recusados. Cadastro e troca de senha
exigem pelo menos oito caracteres, uma letra e um número (RN06). Como limitação técnica do BCrypt atual,
senhas acima de 72 bytes UTF-8 são recusadas para evitar truncamento silencioso.
Essa validação é compartilhada pelo cadastro, edição administrativa e alteração da própria senha.

Após o login aceito, o servidor guarda o id da conta em uma sessão. O navegador
recebe STOCKA_SESSION, um cookie HttpOnly e SameSite=Lax. Não há token para guardar
em localStorage. A sessão fica em memória, expira após 30 minutos de inatividade
e é perdida ao reiniciar a API. Essas são decisões técnicas deste incremento.

Uma tentativa processada pelo controlador invalida a sessão anterior; um login
aceito gera outro identificador. Logout invalida a sessão no servidor, mesmo que
o navegador ainda guarde um cookie antigo. /me reconsulta a conta e o perfil no
banco; desativação ou mudança de perfil é percebida nessa consulta.
As rotas protegidas futuras também deverão verificar conta e permissões.

## Teste manual no PowerShell

Com a API iniciada, use outro terminal:

```powershell
$corpoStocka = @{ login = 'admin@stocka.local'; senha = 'Stocka123' } | ConvertTo-Json
$cabecalhosStocka = @{ 'X-STOCKA-Request' = 'true' }
Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -ContentType 'application/json' -Headers $cabecalhosStocka -Body $corpoStocka -SessionVariable sessaoStocka
Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/me' -WebSession $sessaoStocka
Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/logout' -Method Post -Headers $cabecalhosStocka -WebSession $sessaoStocka
```

Consultar /me novamente com essa sessão após sair retorna 401.

## Testes automatizados

AutenticacaoLocalTest verifica as credenciais usando os exemplos do banco.
ApiLocalTest verifica as rotas por HTTP com servidor e cookies reais. As suítes
só são ativadas com -Dstocka.testesLocais=true, já incluído no script da raiz,
e recusam um banco fora de localhost/127.0.0.1:15432/stocka.
Maven test sem essa opção compila e ignora esses testes dependentes do banco.
A restrição de destino pertence aos testes; a aplicação pode usar PostgreSQL remoto.

Os cenários cobrem contas válidas, senha incorreta, conta inexistente ou desativada,
tratamento do login e da senha, tentativa de injeção SQL, cookie, sessão ausente,
troca de sessão, logout, CORS e revalidação da conta durante a sessão.

Um cenário cria uma conta temporária com login único, altera somente essa conta
para verificar perfil e desativação, e a remove em finally. Nenhum teste altera
as quatro contas do seed ou suas atribuições. A sequência de ids avança normalmente.
Interromper o processo abruptamente pode impedir a limpeza da conta temporária,
cujo login começa com teste-api-. Alterar os exemplos do seed pelo pgAdmin pode
fazer os testes deixarem de corresponder aos dados esperados.

## Hospedagem e configuração HTTP

As opções HTTP estão em application.properties. Variáveis do terminal podem
sobrescrever SERVER_PORT, FRONTEND_ORIGIN e SESSION_COOKIE_SECURE; elas não são
carregadas automaticamente do backend/.env pelo Spring Boot.
A API escuta em 127.0.0.1 para desenvolvimento local. Cookie Secure é false para
HTTP local; um ambiente HTTPS deverá usar true. Uma implantação remota exigirá
revisar endereço de escuta, origem e HTTPS. As sessões atuais ficam em uma instância Java.

Para usar Supabase como hospedagem PostgreSQL, a intenção é alterar somente
DB_URL, DB_USER e DB_PASSWORD, desde que tabelas, tipos, regras e permissões sejam
equivalentes. O banco do colega precisará receber a estrutura SQL atual; a troca
do endereço não transfere os dados nem adapta seu esquema automaticamente.
Os exemplos fictícios do seed são destinados ao desenvolvimento local.

No Supabase, obtenha os parâmetros JDBC em Connect e configure SSL conforme o
provedor. A conexão direta pode exigir IPv6; o Session pooler oferece alternativa
para IPv4. O Transaction pooler tem diferenças que exigem configuração específica.
Usar Supabase Auth, Data API ou políticas RLS desses serviços é outra decisão e
pode exigir mudanças na autenticação. Não foram adotados neste incremento.

## Referências técnicas

- https://docs.spring.io/spring-boot/4.0/system-requirements.html
- https://docs.spring.io/spring-boot/reference/web/servlet.html
- https://supabase.com/docs/guides/database/connecting-to-postgres
- https://maven.apache.org/tools/wrapper/
- https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html

## Consulta dos ambientes autorizados

GET /api/ambientes exige sessão válida e conta ativa. Retorna 200 com uma lista
de objetos contendo id, nome, tipo, descricao e cor; sem ambientes permitidos, retorna [].
Ausência de sessão ou conta desativada retorna 401. Falha SQL segue o tratamento
503 existente. A requisição não recebe identidade ou perfil por parâmetro.

SessaoService centraliza a consulta da conta ativa para /api/auth/me e /api/ambientes.
AutenticacaoController reutiliza essa verificação. AmbienteController recebe a
requisição e passa o id autenticado ao AmbienteDAO. A consulta SQL revalida conta,
perfil e atribuições e devolve somente ambientes ativos, ordenados por nome.

- Administrador: todos os ambientes ativos, independentemente de atribuição.
- Usuário comum: somente ambientes ativos explicitamente atribuídos à conta.
- Conta sem atribuições: lista vazia, com autenticação mantida.
- Revogação de atribuição ou desativação de ambiente: percebida na consulta seguinte.

Ambiente.java é o record dos dados públicos. A coluna cor foi acrescentada pela
migração V002; não alteramos o SQL V001 já aplicado. A busca textual ocorre no Vue sobre a lista autorizada; não há
uma segunda rota de pesquisa com acesso mais amplo.

Os testes HTTP acrescentados verificam essas regras, inclusive tentativa de enviar
id/perfil diferentes na URL e invalidação de conta durante a sessão. Um cenário
cria um ambiente temporário com nome Teste-hub- seguido de UUID, atribui ao operador,
revoga somente essa atribuição e desativa somente esse ambiente. A limpeza em finally
remove o ambiente e suas atribuições temporárias. As salas e atribuições do seed
permanecem intactas. Interrupção abrupta pode impedir essa limpeza; a sequência de
ids avança normalmente. As suítes existentes têm 35 testes locais; o inventário
acrescenta 19 testes de inventário, quatro de senha, 12 de OS e oito de manutenção/resumo e oito de exclusão e oito de gestão de usuários e oito de retirada de ambientes em banco temporário exclusivo, totalizando 102 verificações disponíveis.

O hub também permite gestão administrativa de cadastros e acessos. A rota de inventário
também valida autorização pelo id do ambiente; conhecer um id não concede acesso.

## Cadastro, edição e acessos de ambientes

| Rota | Resultado |
|---|---|
| POST /api/ambientes | 201 com ambiente criado e cabeçalho Location. |
| PUT /api/ambientes/{id} | 200 com ambiente atualizado; 404 se não existe ou está inativo. |
| GET /api/ambientes/{id}/acessos | 200 com id, nome, login, ativo e atribuido dos usuários disponíveis. |
| PUT /api/ambientes/{id}/acessos | 204 após substituir as atribuições deste ambiente. |

Todas essas quatro operações exigem administrador ativo: 401 sem sessão, 403
para usuário comum, inclusive quando a chamada é feita diretamente à API. O
perfil é reconsultado no banco, como nas demais operações de sessão.

O corpo de cadastro/edição contém nome, tipo SALA/SETOR, descricao opcional e cor.
AmbienteDados valida os dados antes de chamar o DAO. Nome é aparado, obrigatório
e limitado a 120 caracteres; duplicidade sem distinguir maiúsculas/minúsculas
retorna 409, garantida pelo índice único do PostgreSQL. Descrição tem limite
técnico de 2000 caracteres. As cores permitidas são TERRACOTA, AZUL, VERDE,
VIOLETA e GRAFITE. Limites, descrição opcional e paleta são decisões técnicas;
a cor é a melhoria visual aprovada pelo usuário, não um novo requisito do ERS.

O corpo de atribuição é {"usuarios":[2,3]}. A lista substitui somente os vínculos
do ambiente indicado; vínculos desses usuários com outros ambientes continuam.
Lista vazia revoga todas as atribuições de usuários deste ambiente, mas não o
acesso geral dos administradores. IDs repetidos são deduplicados. IDs inexistentes,
de administradores ou de contas inativas sem atribuição anterior são rejeitados.
Uma conta inativa já atribuída aparece na lista e pode ser mantida ou removida,
sem recuperar acesso ao sistema por isso.

A substituição usa transação: bloqueia a linha do ambiente, valida todos os IDs,
remove os vínculos anteriores e insere os selecionados. Qualquer falha reverte a
operação inteira. Não retorna hashes e não cria contas. Atribuição/revogação é
percebida pela próxima consulta do hub, sem exigir novo login; não há atualização
automática da lista já aberta em outro navegador.

Os seis novos testes cobrem autorização de escrita e da lista de usuários,
validação, edição, cor persistida, conflito de nome, ambiente inexistente,
atribuição/revogação durante uma sessão, atomicidade em caso de seleção inválida,
preservação/revogação de atribuição inativa e preflight PUT. O teste de mudança
de perfil também verifica perda imediata de acesso às operações administrativas.
As fixtures são ambientes exclusivos removidos em finally; dados do seed são preservados.

RF08/RF09 orientam cadastro; RF10/RF11, atribuições e múltiplos ambientes; RF12,
edição. RF13 está implementado por desativação, com bloqueio por vínculos de
localização/responsabilidade e preservação de registros na V010. A divergência do
subfluxo A2 de RF13 (menciona somente itens ativos) com RN05 permanece como ponto
de revisão documental pelo grupo; o arquivo ERS não foi editado nesta etapa.

## Inventário e auditoria

O backend também oferece listagem/cadastro de itens, foto e consulta de histórico
por ambiente autorizado. ItemDados valida os campos e ItemDAO revalida acesso
dentro da transação de cadastro. V003 associa item e auditoria automaticamente,
preservando autoria e registros imutáveis. Patrimônio duplicado retorna 409.
As rotas, regras, limites e testes isolados estão no [guia do inventário](inventario.md).
As suítes somam 35 verificações existentes, 19 de inventário e quatro de alteração de senha, 12 de OS e oito de manutenção/resumo e oito de exclusão e oito de gestão de usuários e oito de retirada de ambientes. Testar-Backend.ps1
executa as duas etapas; Testar-Inventario.ps1 executa somente o banco temporário.

A edição de itens usa V004, controle de versão e transação com auditoria.
Os scripts de execução/teste limitam a memória das JVMs. Consulte
[Edição e demonstração](edicao-e-demonstracao.md) para contratos e escopo.

Abertura e consulta de OS também estão disponíveis. Consulte [Ordens de serviço](ordens-servico.md).

## Gestão administrativa de contas

UsuarioController autoriza a sessão; GestaoUsuarioDAO valida novamente o
administrador na transação e grava dados com controle de versão. UsuarioDados
valida entrada; UsuarioGestao retorna somente campos públicos. V009 protege o
último administrador e incrementa versões. Veja [Gestão de usuários](gestao-usuarios.md)
para contratos, limites e testes.
