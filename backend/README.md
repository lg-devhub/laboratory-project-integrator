# Backend STOCKA

API Java com Spring Boot e PostgreSQL via JDBC. Autenticação e consulta de ambientes autorizados. O ponto de entrada é ApiApplication.

Na raiz do projeto:

```powershell
.\Iniciar-Banco.ps1
.\Iniciar-API.ps1
```

Para testar as credenciais e a API, use `.\Testar-Backend.ps1` na raiz.

Consulte [o guia do backend](../docs/backend.md) para configuração, responsabilidade
dos arquivos, rotas, sessões e exemplos de requisições.

O backend também atende inventário com fotos, edição/status, auditoria e controle de
versão. Consulte [o guia de edição](../docs/edicao-e-demonstracao.md).
