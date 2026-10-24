$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot 'backend')
try {
    & .\mvnw.cmd --batch-mode --no-transfer-progress spring-boot:run
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel executar a API. Confira a mensagem acima.' }
} finally { Pop-Location }
