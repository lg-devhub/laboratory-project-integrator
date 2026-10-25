param([switch]$Testar)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw 'Docker nao encontrado. Instale o Docker Desktop e inicie-o antes de executar este arquivo.'
    }
    docker info --format '{{.OSType}}' | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Inicie o Docker Desktop com containers Linux e tente novamente.' }

    $taskEnv = Join-Path $PSScriptRoot '.env'
    if (-not (Test-Path -LiteralPath $taskEnv)) {
        $taskPassword = [Guid]::NewGuid().ToString('N')
        [IO.File]::WriteAllText($taskEnv, "POSTGRES_PASSWORD=$taskPassword`n", [Text.UTF8Encoding]::new($false))
    }

    $taskEnvContent = [IO.File]::ReadAllText($taskEnv)
    if ($taskEnvContent -notmatch '(?m)^PGADMIN_EMAIL=') {
        [IO.File]::AppendAllText($taskEnv, "`nPGADMIN_EMAIL=admin@example.com`n", [Text.UTF8Encoding]::new($false))
    }
    if ($taskEnvContent -notmatch '(?m)^PGADMIN_PASSWORD=') {
        $taskAdminPassword = [Guid]::NewGuid().ToString('N')
        [IO.File]::AppendAllText($taskEnv, "PGADMIN_PASSWORD=$taskAdminPassword`n", [Text.UTF8Encoding]::new($false))
    }

    docker compose up -d --wait --wait-timeout 120
    if ($LASTEXITCODE -ne 0) { throw 'O banco nao iniciou. Confira a mensagem acima e docker compose logs db.' }
    & (Join-Path $PSScriptRoot 'Atualizar-Banco.ps1')
    Write-Host 'PostgreSQL STOCKA disponivel em localhost:15432. Banco e usuario: stocka.'
    Write-Host 'A senha de conexao esta no arquivo .env desta pasta.'
    Write-Host 'pgAdmin: http://localhost:15050 . Login nos campos PGADMIN_EMAIL e PGADMIN_PASSWORD do .env.'
    if ($Testar) { & (Join-Path $PSScriptRoot 'Testar-Banco.ps1') }
} finally { Pop-Location }
