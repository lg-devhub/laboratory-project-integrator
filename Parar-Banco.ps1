$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    docker compose stop pgadmin db
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel parar o banco.' }
    Write-Host 'Banco e pgAdmin parados. Os dados foram preservados.'
} finally { Pop-Location }
