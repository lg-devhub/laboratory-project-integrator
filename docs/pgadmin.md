# Consultar o STOCKA pelo pgAdmin

## Abrir o painel

1. Inicie o Docker Desktop com contêineres Linux.
2. No PowerShell da raiz do projeto, execute `.\Iniciar-Banco.ps1`.
3. Abra http://localhost:15050 no navegador.
4. Entre com os valores PGADMIN_EMAIL e PGADMIN_PASSWORD do arquivo `.env` da raiz.
5. No painel esquerdo, expanda Servers > STOCKA > STOCKA local.
6. Se pedir a senha do servidor PostgreSQL, use POSTGRES_PASSWORD do mesmo `.env`.
7. Expanda Databases > stocka > Schemas > public > Tables.

As tabelas deste incremento são usuarios, ambientes e usuario_ambiente.
Clique com o botão direito em uma tabela e selecione View/Edit Data > All Rows
para visualizar seus registros. Para ver seus campos, expanda Columns.
O painel permite editar dados reais do banco local; alterações feitas ali também
serão vistas pela aplicação quando ela estiver integrada.

## Três acessos diferentes

| Acesso | Onde usar | Credenciais |
|---|---|---|
| Painel pgAdmin | Página do navegador | PGADMIN_EMAIL e PGADMIN_PASSWORD do .env. |
| Servidor PostgreSQL | Conexão STOCKA local dentro do painel | Usuário stocka e POSTGRES_PASSWORD do .env. |
| Aplicação STOCKA | Tela Vue, depois da integração | Contas fictícias do seed, descritas em banco-local.md. |

O e-mail inicial admin@example.com é apenas um identificador local do painel;
não é necessário possuir essa caixa de e-mail. A senha é gerada pelo script.
O arquivo `.env` não deve ser compartilhado ou versionado.
As credenciais iniciais do pgAdmin são aplicadas na criação do seu volume;
para trocar a senha depois, utilize as configurações de usuário do próprio painel.
Editar PGADMIN_PASSWORD no .env não altera uma conta já criada.

## O que aparece no Docker Desktop

Em Containers, o grupo stocka-local contém dois serviços:

- db: executa o PostgreSQL, onde estão os registros do STOCKA.
- pgadmin: executa a interface web para consultar esse mesmo PostgreSQL.

Uma imagem é o pacote usado para criar um contêiner (postgres:17 e dpage/pgadmin4:9.18).
Um contêiner é uma instância em execução desse pacote. Um volume guarda os dados
separadamente: stocka-local_stocka_data contém o banco; stocka-local_pgadmin_data
contém as contas e preferências do painel. O segundo volume não é uma cópia do banco.

O compose.yaml reúne a configuração desses serviços. Dentro da rede do Docker,
o pgAdmin alcança o banco pelo nome db e porta 5432. Programas no Windows,
como o backend Java, alcançam o mesmo banco por localhost:15432.
O navegador abre o painel por localhost:15050. Ambos os acessos estão limitados
a este computador.

O Docker também pode executar APIs e outros serviços, mas neste incremento
somente PostgreSQL e pgAdmin rodam nele. O Java executa separadamente; o frontend Vue também será executado fora do Docker.

## Parar e iniciar novamente

Use `.\Parar-Banco.ps1` na raiz para parar os dois serviços preservando os volumes.
Use `.\Iniciar-Banco.ps1` para iniciar novamente. Fechar a aba do navegador não
para o painel nem o banco. Com o motor do Docker Desktop desligado, eles não atendem.

Referência: https://www.pgadmin.org/docs/pgadmin4/latest/container_deployment.html