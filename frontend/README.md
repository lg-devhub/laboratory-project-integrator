# Frontend STOCKA

Vue 3, TypeScript, Vue Router e Pinia.

Nesta pasta, execute:

```powershell
npm ci
npm run dev
```

O frontend atende em http://localhost:5173. Para verificar tipos e compilar, use
`npm run build`. A consulta da sessão precisa da API e do PostgreSQL iniciados.

Consulte [o guia do frontend](../docs/frontend.md) para a hierarquia das pastas,
o papel de cada arquivo e o funcionamento das rotas e da store.
Login e hub Industrial estão integrados à API. O hub mostra somente ambientes autorizados,
com cartões, pesquisa e estados de carregamento, erro e ausência de resultados.
O inventário permite cadastro, detalhes, fotos e edição/status com tratamento de
conflitos. Consulte [o guia de edição](../docs/edicao-e-demonstracao.md).
