$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot 'backend')
try {
    & .\mvnw.cmd --batch-mode --no-transfer-progress test '-Dstocka.testesLocais=true'
    if ($LASTEXITCODE -ne 0) { throw 'Os testes do backend falharam. Confira as mensagens acima.' }
} finally { Pop-Location }
