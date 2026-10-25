$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    $OutputEncoding = [Text.UTF8Encoding]::new($false)
    Get-Content -LiteralPath (Join-Path $PSScriptRoot 'database/tests/acesso_ambientes.sql') -Raw -Encoding UTF8 |
        docker compose exec -T db psql -U stocka -d stocka -v ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) { throw 'A verificacao do banco falhou.' }
} finally { Pop-Location }
