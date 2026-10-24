$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    # V001 e o seed sao exclusivos da primeira inicializacao. V002 e reexecutavel.
    Get-Content -LiteralPath (Join-Path $PSScriptRoot 'database/migrations/V002__cor_ambiente.sql') -Raw |
        docker compose exec -T db psql -X -U stocka -d stocka -v ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao atualizar o esquema. Os dados nao foram apagados.' }
    Write-Host 'Esquema STOCKA atualizado; dados existentes preservados.'
} finally { Pop-Location }
