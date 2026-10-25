# Frontend STOCKA

Vue 3, TypeScript, Vue Router e Pinia. O login usa a direção visual Industrial
aprovada: base mineral clara, acento terracota e composição em dois painéis.
Login e hub estão integrados à API Java. Os cartões apresentam nome, tipo e descrição
dos ambientes permitidos para a conta, com pesquisa e saída. O administrador também cadastra/edita ambientes e gerencia acessos.

## Executar

Dentro da pasta frontend:

```powershell
npm ci
npm run dev
```

O endereço é http://localhost:5173. O servidor usa porta fixa e falha se ela estiver
ocupada, pois a API permite explicitamente essa origem. Use localhost na URL.
O projeto exige Node ^22.18.0 ou >=24.12.0; neste computador foi usado Node 24.13.1.
Se as dependências já estiverem instaladas, basta npm run dev.

Para verificar os tipos TypeScript e gerar a versão compilada:

```powershell
npm run build
```

Para a consulta de sessão funcionar, inicie banco e API em outro terminal,
a partir da raiz do projeto, com Iniciar-Banco.ps1 e Iniciar-API.ps1.
Sem API, a view de login mostra um aviso de indisponibilidade. Isso não impede
a compilação da interface. O formulário já permite entrar com as contas locais e encaminha para /ambientes.

## Estrutura

```text
src/
├── assets/
│   └── styles/main.css
├── components/
│   ├── AlterarSenha.vue
│   ├── MarcaStocka.vue
│   ├── AmbienteCard.vue
│   ├── AmbienteFormulario.vue
│   ├── AmbienteAcessos.vue
│   ├── OrdemServicoEdicao.vue
│   ├── OrdensServico.vue
│   ├── InventarioResumo.vue
│   ├── Manutencoes.vue
│   ├── ItemFormulario.vue
│   └── ItemDetalhes.vue
├── views/
│   ├── LoginView.vue
│   ├── AmbientesView.vue
│   └── InventarioView.vue
├── router/index.ts
├── services/
│   ├── api.ts
│   ├── autenticacao.ts
│   ├── ambientes.ts
│   └── itens.ts
├── stores/autenticacao.ts
├── types/
│   ├── usuario.ts
│   ├── ambiente.ts
│   └── item.ts
├── App.vue
└── main.ts
```

| Parte | Responsabilidade |
|---|---|
| main.ts | Criar a aplicação e registrar Pinia, router e estilo global. |
| App.vue | Apresentar a view correspondente à rota com RouterView. |
| views | Telas associadas aos endereços da aplicação. |
| components | Partes reutilizáveis da interface, criadas conforme o design e a necessidade. |
| router | Definir rotas e verificar a sessão antes de navegar. |
| services/api.ts | Endereço do backend, cookie, cabeçalhos HTTP e limite de espera de oito segundos. |
| services/autenticacao.ts | Requisições de login, consulta da sessão e logout. |
| stores/autenticacao.ts | Compartilhar a conta autenticada, a verificação e seus erros entre telas. |
| types/usuario.ts | Descrever os dados públicos da conta retornados pela API. |
| assets | Imagens importadas pelo Vue e estilos da interface. |

MarcaStocka é usado no login e no cabeçalho. AmbienteCard representa cada sala ou setor.
A pasta vazia assets/images foi removida; fotos dos itens ficam no banco. Não é preciso criar componentes
antes de identificarmos sua utilidade. public continua recebendo arquivos servidos
diretamente, como o favicon. package-lock.json registra as versões de dependências;
node_modules e dist são gerados pelas ferramentas e ficam fora do Git.

## Padrões propostos

- Views e componentes Vue usam PascalCase: LoginView.vue, AmbientesView.vue.
- Views têm o sufixo View para distinguir telas de componentes reutilizáveis.
- Pastas e arquivos TypeScript usam nomes descritivos em minúsculas.
- Imports internos podem usar @/ como referência à pasta src.
- Campos de formulário e erros locais ficam na view; dados compartilhados ficam na store.
- Requisições HTTP ficam em services; as views não repetem a configuração de fetch.
- CSS global fica em assets/styles; estilos específicos futuros podem usar style scoped na view ou componente.
- Não são criadas pastas de módulos futuros antes de haver código para eles.

Esses padrões são decisões técnicas de organização, não requisitos da empresa.

## Rotas e sessão

- / redireciona para /login.
- /login mostra o formulário de entrada; com sessão válida, redireciona para /ambientes.
- /ambientes exige sessão válida e consulta a lista autorizada na API.
- Endereços desconhecidos redirecionam para /login, passando pela mesma verificação.

A cada navegação, o router chama a store para consultar GET /api/auth/me. Uma
resposta 401 significa ausência de sessão e limpa o usuário local. Falhas de rede
ou de servidor impedem o acesso à rota protegida e são informadas na view de login.
O carregamento inicial aguarda essa consulta, limitada a oito segundos.

A store começa vazia ao atualizar a página e recupera a conta pelo cookie enviado
automaticamente pelo navegador. Não grava senha, hash ou token em localStorage.
O formulário chama a ação de entrar; o botão Sair do hub encerra a sessão. O backend continua decidindo autenticação e permissões; o router
apenas organiza a navegação. As permissões dos ambientes são aplicadas pela rota Java, não pelo filtro da interface.

O fluxo do formulário é: LoginView chama a ação entrar da store; a ação
usa o serviço de autenticação, atualiza a conta com a resposta e a view solicita
a navegação para AmbientesView. O serviço consulta a API; a store guarda o resultado.

## Configuração

O endereço padrão da API é http://localhost:8080. Se necessário, copie
frontend/.env.example para frontend/.env e altere VITE_API_URL. Reinicie o servidor
Vite após mudar essa configuração. Variáveis VITE_ ficam disponíveis no navegador;
nunca coloque nelas credenciais do PostgreSQL ou segredos.
Esse arquivo de configuração é independente dos .env da raiz e do backend.

O helper HTTP envia credentials: include para o cookie de sessão e acrescenta
X-STOCKA-Request: true nas requisições de escrita. Os serviços usam o contrato
atual da API: login e senha na entrada; id, nome, login e perfil no resultado.
Veja [o guia do backend](backend.md) para os detalhes do contrato.

## Identidade visual Industrial

As variáveis de cor e o estilo comum dos botões ficam em assets/styles/main.css.
A tela usa Segoe UI com fontes de sistema como alternativa, sem carregar fontes
externas. O tema inicial é claro. As variáveis são escolhas técnicas de apresentação,
não requisitos de negócio: base #faf8f4, painel #eae7de e ação principal #b64c1d.

LoginView mantém a composição e o CSS específico do login. MarcaStocka concentra
o nome e o símbolo de referência. Os ícones são importados individualmente de
@lucide/vue. O símbolo atual e o favicon seguem a referência visual aprovada;
o desenho de uma logo definitiva ainda poderá ser refinado.

Em telas estreitas, os painéis se empilham e os elementos decorativos são removidos.
Os campos têm labels e autocomplete adequados; a senha pode ser mostrada ou ocultada.
Durante o envio, o formulário bloqueia novos envios e informa Entrando. Erros de
acesso e indisponibilidade são mostrados junto ao formulário. Nenhuma senha ou
credencial de banco é gravada na store ou em localStorage.

A validação em navegador foi realizada com a API e o banco locais reais: contas
administrador, operador e sem atribuições; senha incorreta, conta inexistente ou
desativada; cookie de sessão, recarga e saída. A compilação verifica os tipos
TypeScript. O layout foi conferido em desktop e larguras de 390 e 320 pixels.

## Próxima etapa

Refinar a base funcional quando necessário e definir o conteúdo interno de cada ambiente.
A consulta e a pesquisa do hub já usam os dados reais da API.
O layout anterior e a base provisória do frontend ficam preservados no histórico externo.

Referências: https://router.vuejs.org/guide/, https://pinia.vuejs.org/introduction.html
e https://lucide.dev/guide/vue/getting-started.
## Hub de ambientes integrado

AmbientesView chama services/ambientes.ts ao montar. O serviço consulta
GET /api/ambientes com o cookie da sessão. types/ambiente.ts
descreve id, nome, tipo SALA/SETOR, descrição opcional e cor. AmbienteCard apresenta
esses dados e emite ações administrativas; a view coordena carregamento, pesquisa, erros e saída.

A pesquisa filtra a lista já autorizada pelo backend, por nome, tipo ou descrição,
ignorando maiúsculas e acentos. Não busca usuários ou ambientes aos quais a conta
não tem acesso. A contagem exibida é a quantidade de ambientes encontrados,
nunca uma quantidade fictícia de itens.

Há estados distintos para carregamento, falha com botão Tentar novamente, conta
sem ambientes disponíveis e pesquisa sem resultado com opção Limpar pesquisa.
Uma resposta 401 limpa a conta local e encaminha ao login. Um erro 503 não é
tratado como lista vazia; a pessoa pode tentar novamente.

Os cartões têm ações de edição e acessos para administradores e Abrir inventário
para as contas autorizadas. A página interna lista, pesquisa e cadastra itens,
com detalhes, foto e auditoria. Não há contagem de itens nos cartões, gráficos
ou abertura de OS nesta etapa. Veja [o guia do inventário](inventario.md).

Validação em navegador: administrador com dois ambientes de exemplo, operador
com um, conta sem atribuições com zero, pesquisa, carregamento, repetição após erro,
sessão expirada e larguras de 390 e 320 pixels. A falha 503 foi simulada; a consulta,
a autenticação e as permissões usaram Java e PostgreSQL reais.

## Gestão administrativa do hub

- AmbientesView coordena a lista, o formulário aberto, mensagens de resultado e
  atualização local após salvar. O perfil na store controla a apresentação dos botões;
  o backend decide a autorização real.
- AmbienteCard recebe o ambiente e a indicação de administrador. A cor aparece na
  faixa superior, fundo suave e ícone; nome/tipo continuam identificáveis sem depender da cor.
  Botões explícitos Editar e Acessos evitam um menu adicional neste incremento.
- AmbienteFormulario contém os campos de cadastro/edição e a paleta fixa. O mesmo
  componente atende às duas operações, sem duplicar formulário.
- AmbienteAcessos carrega os usuários disponíveis e permite selecionar múltiplos
  vínculos ou revogá-los. Administradores já têm acesso geral e não entram na seleção.
- services/ambientes.ts concentra GET/POST/PUT. ErroAmbiente conserva o status HTTP
  para distinguir sessão expirada, acesso negado e falhas de validação.
- types/ambiente.ts contém os contratos e a correspondência da paleta com cores CSS.

Os formulários usam dialog nativo: foco fica no diálogo, Escape cancela e o foco
retorna ao botão de origem. Durante o envio, campos e botões são bloqueados e
Escape não fecha o diálogo. Erros não descartam o preenchimento. Uma sessão expirada
encaminha ao login. O estilo compartilhado dos diálogos fica em main.css; estilos
específicos permanecem nos componentes. Não foram criadas stores ou dependências novas.

A cor é compartilhada por ambiente e persistida no PostgreSQL. É uma melhoria
visual aprovada, sem significado de permissão ou status. Animações e refinamentos
visuais gerais ficaram para depois. A descrição opcional segue a convenção técnica
já adotada, e não uma exigência textual adicional do ERS.

Verificação desta etapa: compilação TypeScript/Vite; navegador com API e banco reais,
cadastro/edição, nome duplicado sem fechar formulário, persistência após recarga,
atribuição e revogação verificadas em uma segunda sessão, conta comum sem botões
administrativos, expiração de sessão, Escape e restauração de foco. Layout conferido
em 1440, 390 e 320 pixels. O ambiente temporário do navegador foi removido ao final.

## Inventário após o hub

/ambientes/:ambienteId usa InventarioView.vue, protegida pelo router e pela API.
AmbienteCard oferece links para o inventário. ItemFormulario.vue concentra cadastro;
ItemDetalhes.vue mostra dados, foto e histórico. services/itens.ts mantém as chamadas
HTTP e types/item.ts seus contratos. Os dados da lista ficam na view, sem nova store.
Pesquisa abrange nome, categoria, patrimônio, identificação e localização; filtro de
status é uma conveniência adicional. A contagem representa registros, não soma de
unidades de naturezas diferentes. Detalhes completos e limites estão em [Inventário](inventario.md).

A seção de [ordens de serviço](ordens-servico.md) aparece nos detalhes de equipamentos,
com lista expansível e abertura integrada ao Java/PostgreSQL.

Manutencoes.vue registra e consulta serviços; InventarioResumo.vue mostra indicadores
por status e permite filtrar registros. Veja [Manutenções e indicadores](manutencoes.md).
