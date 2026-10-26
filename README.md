# STOCKA

Sistema acadêmico de controle de itens e equipamentos por salas e setores, com
ordens de serviço, registros de manutenção e acesso conforme o perfil do usuário.
Desenvolvido a partir da problemática da empresa e do ERS aprovado pelo grupo.

O fluxo integrado é **Vue → API Java → PostgreSQL**. O banco e o pgAdmin executam
no Docker; a API e o frontend executam separadamente no computador.

## Funcionalidades disponíveis

| Área | Implementação atual |
|---|---|
| Autenticação | Login, logout, recuperação da sessão e alteração da própria senha. Senhas com hash BCrypt e sessão por cookie HttpOnly. |
| Usuários | Administrador cadastra, edita dados/perfil, redefine senhas e desativa contas, preservando autoria e o último administrador ativo. |
| Ambientes | Cadastro e edição de salas/setores, cor de identificação, pesquisa e atribuição/revogação de acessos. |
| Retirada de ambientes | Desativação administrativa com confirmação e bloqueio por vínculos de localização ou responsabilidade, inclusive de itens excluídos logicamente. |
| Inventário | Cadastro individual ou por quantidade, pesquisa, filtro por status, edição, foto opcional armazenada no banco e auditoria automática. |
| Exclusão de itens | Exclusão lógica com confirmação, auditoria e bloqueio por OS pendente. Administrador pode consultar os excluídos e seus históricos. |
| Ordens de serviço | Abertura, consulta, atualização e avanço de Aberta para Em Andamento e Concluída. Atendimento obrigatório ao concluir, fechamento automático e concluídas somente para consulta. |
| Manutenções | Registro e consulta dos serviços, com responsável técnico, autor identificado pela sessão e vínculo opcional com OS. |
| Indicadores | Contagens reais por status dos itens e por situação das OS no inventário. |

Administradores acessam todos os ambientes ativos; usuários comuns acessam somente
os ambientes atribuídos. A API verifica as permissões em cada operação.

**Localização e responsabilidade são vínculos distintos.** Todo item possui um
ambiente atual. Equipamentos individuais também possuem patrimônio único e um
setor responsável, que pode ser diferente da localização. A responsabilidade não
concede acesso ao inventário de outro ambiente nem duplica o equipamento.

O status do equipamento é independente da situação da OS. Concluir uma OS não
altera esse status nem cria automaticamente um registro de manutenção.

## Tecnologias e organização

- **Frontend:** Vue 3, TypeScript, Vue Router, Pinia e Vite.
- **Backend:** Java 21+, Spring Boot e JDBC.
- **Banco:** PostgreSQL 17, Docker Compose e pgAdmin.
- **Testes:** JUnit, testes HTTP da API e verificações SQL.

| Caminho | Responsabilidade |
|---|---|
| [frontend/](frontend/) | Views, componentes, rotas, serviços HTTP e estado da autenticação. |
| [backend/](backend/) | API Java, validações, sessões, acesso ao banco e testes de integração. |
| [database/](database/) | Migrações SQL, dados fictícios, consultas e testes do banco. |
| [docs/](docs/) | Guias técnicos, contratos da API, regras e decisões do projeto. |
| [compose.yaml](compose.yaml) | Serviços locais de PostgreSQL e pgAdmin, com volume persistente. |
| `.env` | Configuração local do Docker e senhas de PostgreSQL/pgAdmin. |
| `backend/.env` | Configuração da conexão da API com o PostgreSQL. |

Os arquivos `.env` são locais e ficam fora do Git. Os exemplos de configuração são
versionados. `node_modules/`, `dist/` e `target/` são gerados pelas ferramentas e
também ficam fora do versionamento.

## Requisitos para executar

O procedimento abaixo usa Windows e PowerShell:

- Git, para clonar e atualizar o repositório.
- Docker Desktop iniciado, com contêineres Linux e Docker Compose disponível.
- JDK 21 ou superior, com `java` disponível no terminal.
- Node.js `^22.18.0` ou `>=24.12.0`, com npm.

O Maven Wrapper está incluído no backend; não é necessário instalar Maven
separadamente. A primeira execução precisa de internet para baixar dependências
e imagens do Docker.

## Primeira execução

### 1. Obter o projeto e iniciar o banco

```powershell
git clone https://github.com/lg-devhub/laboratory-project-integrator.git
cd laboratory-project-integrator
.\Iniciar-Banco.ps1
```

Se o projeto já estiver no computador, abra o terminal na pasta que contém
`compose.yaml` e execute apenas o script de inicialização.

O script gera o `.env` da raiz quando ele não existe, inicia PostgreSQL e pgAdmin
e aplica as atualizações de estrutura. Em um banco novo, o Docker cria as tabelas
e insere as contas e os ambientes fictícios iniciais.

### 2. Configurar a conexão da API

Somente na primeira configuração, copie o arquivo de exemplo:

```powershell
Copy-Item .\backend\.env.example .\backend\.env
```

Edite `backend/.env`, mantendo a URL e o usuário locais do exemplo:

```dotenv
DB_URL=jdbc:postgresql://localhost:15432/stocka?connectTimeout=5&socketTimeout=10
DB_USER=stocka
DB_PASSWORD=substitua_pelo_valor_de_POSTGRES_PASSWORD
```

Preencha `DB_PASSWORD` com o valor de `POSTGRES_PASSWORD` gerado no `.env` da raiz.
Essa senha é de conexão com o PostgreSQL; ela não é a senha de login do STOCKA.
Não substitua um `backend/.env` já configurado ao executar novamente o projeto.

### 3. Iniciar a API

No terminal da raiz:

```powershell
.\Iniciar-API.ps1
```

Mantenha esse terminal aberto. A API atende em `http://localhost:8080`.

### 4. Iniciar o frontend

Em outro terminal, a partir da raiz:

```powershell
cd frontend
npm ci
npm run dev
```

Acesse **http://localhost:5173**. Use `localhost` na URL, pois a origem padrão
permitida pela API é `http://localhost:5173`.

### Contas de teste

As contas abaixo são criadas por [database/seed/local.sql](database/seed/local.sql)
somente na inicialização de um banco novo. A senha inicial de todas é `Stocka123`.

| Login | Perfil e situação inicial |
|---|---|
| `admin@stocka.local` | Administrador ativo. |
| `operador@stocka.local` | Usuário ativo com acesso ao setor Produção. |
| `semacesso@stocka.local` | Usuário ativo sem ambientes atribuídos. |
| `inativo@stocka.local` | Usuário desativado; login bloqueado. |

No banco, as senhas são armazenadas como hash. Se uma senha for alterada pela
aplicação, as próximas inicializações preservam essa alteração.

Cada computador possui seu próprio banco local. Clonar ou atualizar o repositório
não copia usuários, itens, fotos ou demais dados cadastrados em outra máquina.

## Uso diário e endereços

Com a configuração inicial pronta, inicie Docker Desktop e execute na raiz:

```powershell
.\Iniciar-Banco.ps1
.\Iniciar-API.ps1
```

Em outro terminal, na pasta `frontend`, execute `npm run dev`. Repita `npm ci`
quando precisar instalar as dependências de uma versão atualizada do projeto.

| Serviço | Endereço / acesso |
|---|---|
| Site Vue | http://localhost:5173 |
| API Java | http://localhost:8080 |
| PostgreSQL | `localhost:15432`, banco `stocka`, usuário `stocka` |
| pgAdmin | http://localhost:15050 — credenciais `PGADMIN_EMAIL` e `PGADMIN_PASSWORD` do `.env` da raiz |

A API não possui uma tela inicial. `GET /api/auth/me` retorna `401` quando não há
sessão autenticada; isso é esperado. O pgAdmin usa credenciais próprias, diferentes
das contas do STOCKA. Veja [como consultar as tabelas](docs/pgadmin.md).

Para encerrar, use `Ctrl+C` nos terminais do Vue e da API. Para parar o banco:

```powershell
.\Parar-Banco.ps1
```

Se necessário, `Parar-API.ps1` encerra uma API reconhecida como pertencente a este
projeto. O banco fica em um volume persistente: parar os serviços não apaga dados.

## Atualizações e problemas comuns

- **Após atualizar o código:** execute `Iniciar-Banco.ps1` ou, com o banco já ativo,
  `Atualizar-Banco.ps1`, e reinicie a API. As migrações V002–V010 são reexecutáveis.
- **Porta 8080 ou 5173 ocupada:** confira se a API ou o Vue já estão executando em
  outro terminal antes de iniciar uma segunda instância.
- **Falha de conexão com o banco:** confira Docker Desktop, a porta `15432` e a
  correspondência entre `backend/.env` e a senha configurada no PostgreSQL.
- **Alterar `.env` não troca a senha de um banco existente.** A configuração do
  Docker e a conexão do Java são independentes; os dois arquivos não se sincronizam.

Não é necessário apagar volumes ou recriar o banco para aplicar as migrações.

## Testes e verificação

Com o banco iniciado e a conexão da API configurada, execute na raiz:

```powershell
.\Testar-Banco.ps1
.\Testar-Backend.ps1
```

| Comando | Verificação |
|---|---|
| `Testar-Banco.ps1` | Restrições e consultas SQL de usuários, ambientes e atribuições. |
| `Testar-Backend.ps1` | Conjunto de 102 testes automatizados de autenticação, ambientes, inventário, senha, OS, manutenção, exclusão de itens e gestão de usuários/ambientes. |
| `Testar-Inventario.ps1` | Subconjunto de 67 testes em banco temporário exclusivo, removido ao final. |

Os testes HTTP iniciam e encerram sua própria API em porta livre; não é preciso
iniciar a API de desenvolvimento para executá-los. Os 35 testes iniciais de
credenciais/ambientes usam o banco local e suas contas de exemplo, com fixtures
próprias para as escritas. Alterar essas contas pode mudar os resultados esperados.

Os relatórios Java são gerados em `backend/target/surefire-reports/` e não são
versionados. A última regressão do conjunto de 67 testes passou sem falhas.
As telas também foram conferidas em larguras de 1440, 390 e 320 pixels.

Para verificar TypeScript e gerar o build do frontend, na pasta `frontend`:

```powershell
npm run build
```

Para uma demonstração com mais registros, `Popular-Banco.ps1`, na raiz e com a API
ativa, acrescenta quatro ambientes DEMO e 12 itens fictícios. Consulte
[o guia de demonstração](docs/edicao-e-demonstracao.md) para credenciais, reexecução
e preservação dos dados existentes.

## Documentação

O [índice dos guias](docs/README.md) reúne a documentação técnica. Os contratos da
API são descritos nos guias abaixo, com rotas, entradas, respostas e permissões.

| Guia | Conteúdo |
|---|---|
| [Backend e autenticação](docs/backend.md) | Configuração, sessões, login e contratos de ambientes/acessos. |
| [Inventário](docs/inventario.md) | Itens, fotos, auditoria, validações e API. |
| [Ordens de serviço](docs/ordens-servico.md) | Abertura, consulta, atualização e encerramento. |
| [Manutenções](docs/manutencoes.md) | Registro de serviços e indicadores. |
| [Gestão de usuários](docs/gestao-usuarios.md) | Cadastro, edição, senha, desativação e último administrador. |
| [Exclusão de itens](docs/exclusao-itens.md) | Exclusão lógica e preservação dos históricos. |
| [Retirada de ambientes](docs/retirada-ambientes.md) | Confirmação, vínculos impeditivos e desativação. |
| [Modelo do banco](docs/modelo-banco.md) | Tabelas, campos, relações e migrações. |
| [Frontend](docs/frontend.md) | Organização das pastas, componentes, rotas e serviços. |
| [Decisões e próximos passos](docs/decisoes-e-proximos-passos.md) | Regras acordadas, escolhas técnicas e trabalho futuro. |

## Escopo atual

O sistema possui fluxos integrados e funcionais para o escopo descrito neste
README. O cadastro de múltiplos registros em uma operação (RF24) e a movimentação
de equipamentos (RF33) continuam pendentes. Informar quantidade em um único
registro não equivale ao cadastro de vários registros distintos.

O ERS aprovado pelo grupo é a referência dos requisitos. As escolhas de interface,
organização e implementação estão registradas nos guias técnicos. A versão final
do ERS e os diagramas aprovados ainda precisam ser reunidos no repositório;
o roteiro de modelagem está em [Guia dos diagramas](docs/guia-diagramas-equipe.md).
