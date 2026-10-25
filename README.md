# STOCKA

Projeto acadêmico com frontend Vue, API Java e PostgreSQL local.
O fluxo disponível é login e hub com ambientes autorizados. Administradores podem cadastrar, editar e gerenciar acessos aos ambientes.
Salas e setores podem ser [retirados de uso](docs/retirada-ambientes.md) pelo administrador quando estiverem sem vínculos com itens/equipamentos.
Administradores também podem [cadastrar, editar e desativar usuários](docs/gestao-usuarios.md), preservando históricos.
Cada ambiente oferece inventário com cadastro/edição, fotos, auditoria, OS e registro/consulta de manutenções de equipamentos.

## Organização

| Pasta ou arquivo | Responsabilidade |
|---|---|
| frontend/ | Login, hub e inventário com identidade Industrial, integrados à API. |
| backend/ | API Java: autenticação, ambientes/acessos, cadastro e consulta do inventário. |
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
| .\Atualizar-Banco.ps1 | Aplicar V002–V010 no banco existente sem apagar dados. |
| .\Parar-Banco.ps1 | Parar os dois serviços preservando seus dados. |
| .\Testar-Banco.ps1 | Verificar as restrições e consultas SQL do banco local. |
| .\Iniciar-API.ps1 | Iniciar a API Java. |
| .\Parar-API.ps1 | Parar somente a API Java deste projeto, inclusive se iniciada em segundo plano. |
| .\Testar-Backend.ps1 | Executar os testes locais de credenciais e da API. |
| .\Testar-Inventario.ps1 | Testar inventário e auditoria em banco temporário exclusivo. |

Os testes precisam do banco iniciado. Os testes HTTP criam e encerram sua própria
API em porta livre; não é necessário iniciar outra API manualmente para testá-los.

## Estado atual

- O banco contém usuários, ambientes e atribuições entre eles.
- O inventário contém itens e auditoria automática, com foto opcional e responsabilidade separada da localização.
- A API oferece login, consulta da conta, logout, listagem, cadastro e edição de ambientes e gerenciamento de atribuições.
- A senha é conferida com BCrypt; a sessão usa cookie HttpOnly.
- Ambos os perfis podem alterar a própria senha no hub, informando a senha atual e respeitando RF03/RN06. Veja [o guia](docs/alteracao-senha.md).
- O login Vue está integrado à API, com identidade Industrial, recuperação da sessão e saída.
- O hub lista salas e setores autorizados e permite pesquisar por nome, tipo ou descrição.
- Administradores cadastram/editam ambientes, escolhem sua cor e atribuem/revogam acessos de usuários. A API verifica o perfil em cada ação.
- Retirada de ambientes ainda exige implementar as verificações da RN05; não há exclusão disponível.

O fluxo completo será: Vue envia requisições à API Java; o Java verifica a conta
e as permissões, consulta PostgreSQL e devolve os dados que a interface apresenta.

## Documentação e próximos passos

Comece pelo [índice da documentação](docs/README.md). Os guias descrevem a versão
atual; o material do protótipo e os guias substituídos foram arquivados fora do projeto.
A ERS revisada continua sendo a referência de requisitos e ainda precisa ser
incluída no repositório na versão aprovada pelo grupo.

O login Industrial já está integrado à API. Consulte [o guia do frontend](docs/frontend.md).
A base do hub e o primeiro inventário estão funcionais: abra um ambiente para listar,
pesquisar, cadastrar e editar itens/status, consultar foto e auditoria. Localização e setor responsável
são distintos. Veja [o guia do inventário](docs/inventario.md) e o registro de
[decisões e próximos passos](docs/decisoes-e-proximos-passos.md). Exclusão,
movimentação segue para incremento posterior; registro e consulta de manutenção estão disponíveis. Abertura, consulta, andamento e encerramento de OS
para equipamentos já estão disponíveis.

As pastas target, node_modules e dist, quando presentes, são geradas pelas
ferramentas e ficam fora do Git. Não representam código que o grupo precisa manter.

Para acrescentar quatro ambientes DEMO e 12 itens fictícios ao banco local, com a API
ativa, execute .\Popular-Banco.ps1 na raiz. Reexecução não duplica os exemplos.
Veja [Edição e demonstração](docs/edicao-e-demonstracao.md) para funcionamento e limites.

Consulte [Ordens de serviço](docs/ordens-servico.md) para abertura, consulta e escopo atual.

Registro e histórico de manutenção, indicadores por status e contagens de OS estão
em [Manutenções e leitura do inventário](docs/manutencoes.md).

Itens agora permitem exclusão lógica com confirmação, auditoria e consulta administrativa.
Veja [Exclusão de itens](docs/exclusao-itens.md). Retirada de ambientes e desativação de usuários seguem pendentes.
