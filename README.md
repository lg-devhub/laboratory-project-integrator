# STOCKA

Projeto acadêmico com frontend Vue, API Java e PostgreSQL local.
O fluxo disponível é login e hub com ambientes autorizados. Administradores podem cadastrar, editar e gerenciar acessos aos ambientes.

## Organização

| Pasta ou arquivo | Responsabilidade |
|---|---|
| frontend/ | Login e hub Industrial integrados à API, com pesquisa de ambientes. |
| backend/ | API Java: autenticação, sessão e gestão de ambientes/acessos. |
| database/ | Estrutura SQL, dados fictícios, consultas, testes e conexão do pgAdmin. |
| docs/ | Guias atuais do projeto e modelo do banco. |
| compose.yaml | Execução local do PostgreSQL e do pgAdmin pelo Docker. |
| .env e backend/.env | Configurações locais; ficam fora do Git. |

## Executar

Com JDK 21 ou superior instalado e Docker Desktop iniciado, execute no PowerShell
da raiz do projeto:

```powershell
.\Iniciar-Banco.ps1
.\Iniciar-API.ps1
```

Mantenha o terminal da API aberto. Ctrl+C encerra a API.
Em uma máquina nova, configure primeiro backend/.env conforme [o guia do backend](docs/backend.md).
O Maven Wrapper baixa as dependências necessárias, sem exigir instalação manual do Maven.

| Serviço | Endereço |
|---|---|
| PostgreSQL | localhost:15432, banco e usuário stocka |
| pgAdmin | http://localhost:15050 |
| API Java | http://localhost:8080/api/auth/me |

A consulta /me retorna 401 enquanto não houver uma sessão autenticada.
O login e o logout são requisições POST; abrir a raiz da API no navegador não exibe uma tela.
Para visualizar as tabelas, consulte [o guia do pgAdmin](docs/pgadmin.md).

## Comandos disponíveis

| Comando na raiz | Função |
|---|---|
| .\Iniciar-Banco.ps1 | Iniciar PostgreSQL e pgAdmin. |
| .\Atualizar-Banco.ps1 | Aplicar V002 no banco existente sem apagar dados. |
| .\Parar-Banco.ps1 | Parar os dois serviços preservando seus dados. |
| .\Testar-Banco.ps1 | Verificar as restrições e consultas SQL do banco local. |
| .\Iniciar-API.ps1 | Iniciar a API Java. |
| .\Testar-Backend.ps1 | Executar os testes locais de credenciais e da API. |

Os testes precisam do banco iniciado. Os testes HTTP criam e encerram sua própria
API em porta livre; não é necessário iniciar outra API manualmente para testá-los.

## Estado atual

- O banco contém usuários, ambientes e atribuições entre eles.
- A API oferece login, consulta da conta, logout, listagem, cadastro e edição de ambientes e gerenciamento de atribuições.
- A senha é conferida com BCrypt; a sessão usa cookie HttpOnly.
- O login Vue está integrado à API, com identidade Industrial, recuperação da sessão e saída.
- O hub lista salas e setores autorizados e permite pesquisar por nome, tipo ou descrição.
- Administradores cadastram/editam ambientes, escolhem sua cor e atribuem/revogam acessos de usuários. A API verifica o perfil em cada ação.
- Retirada de ambientes ainda depende dos módulos de itens/equipamentos para cumprir a RN05; não há exclusão disponível.

O fluxo completo será: Vue envia requisições à API Java; o Java verifica a conta
e as permissões, consulta PostgreSQL e devolve os dados que a interface apresenta.

## Documentação e próximos passos

Comece pelo [índice da documentação](docs/README.md). Os guias descrevem a versão
atual; o material do protótipo e os guias substituídos foram arquivados fora do projeto.
A ERS revisada continua sendo a referência de requisitos e ainda precisa ser
incluída no repositório na versão aprovada pelo grupo.

O login Industrial já está integrado à API. Consulte [o guia do frontend](docs/frontend.md).
A base do hub está funcional. O conteúdo interno dos ambientes e os módulos de itens e OS serão desenvolvidos em incrementos posteriores.

As pastas target, node_modules e dist, quando presentes, são geradas pelas
ferramentas e ficam fora do Git. Não representam código que o grupo precisa manter.
