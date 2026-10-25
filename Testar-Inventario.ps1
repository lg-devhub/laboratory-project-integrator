$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
$taskBancoTeste = 'stocka_testes_inventario_' + [Guid]::NewGuid().ToString('N').Substring(0,12)
$taskUrlAnterior = $env:DB_URL
$taskMavenAnterior = $env:MAVEN_OPTS
$taskBancoCriado = $false
try {
    # Banco exclusivo: a auditoria nao e apagada para limpar fixtures.
    docker compose exec -T db createdb -U stocka $taskBancoTeste
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel criar o banco temporario.' }
    $taskBancoCriado = $true
    foreach ($taskSql in @('migrations/V001__acesso_ambientes.sql','seed/local.sql','migrations/V002__cor_ambiente.sql','migrations/V003__inventario.sql','migrations/V004__edicao_auditoria_itens.sql')) {
        Get-Content -LiteralPath (Join-Path $PSScriptRoot "database/$taskSql") -Raw |
            docker compose exec -T db psql -X -U stocka -d $taskBancoTeste -v ON_ERROR_STOP=1
        if ($LASTEXITCODE -ne 0) { throw "Falha preparando teste: $taskSql" }
    }
    # Confere que V003 pode ser reaplicada sem reinicializar os dados.
    Get-Content -LiteralPath (Join-Path $PSScriptRoot 'database/migrations/V003__inventario.sql') -Raw |
        docker compose exec -T db psql -X -U stocka -d $taskBancoTeste -v ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao reaplicar V003 no banco temporario.' }
    Get-Content -LiteralPath (Join-Path $PSScriptRoot 'database/migrations/V004__edicao_auditoria_itens.sql') -Raw |
        docker compose exec -T db psql -X -U stocka -d $taskBancoTeste -v ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao reaplicar V004 no banco temporario.' }
    $env:DB_URL = "jdbc:postgresql://localhost:15432/${taskBancoTeste}?connectTimeout=5&socketTimeout=10"
    Push-Location (Join-Path $PSScriptRoot 'backend')
    try {
        if (-not $env:MAVEN_OPTS) { $env:MAVEN_OPTS = '-Xms32m -Xmx192m' }
        & .\mvnw.cmd --batch-mode --no-transfer-progress test '-Dtest=InventarioLocalTest,AlteracaoSenhaLocalTest' '-Dstocka.testesInventario=true' '-DargLine=-Xms32m -Xmx256m -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=64m'
        if ($LASTEXITCODE -ne 0) { throw 'Os testes do inventario falharam.' }
    } finally { Pop-Location }
} finally {
    $env:DB_URL = $taskUrlAnterior
    $env:MAVEN_OPTS = $taskMavenAnterior
    if ($taskBancoCriado) {
        # Nunca aceita o nome do banco de trabalho, mesmo se este script for alterado.
        if ($taskBancoTeste -notmatch '^stocka_testes_inventario_[a-f0-9]{12}$') { throw 'Nome de banco temporario invalido.' }
        docker compose exec -T db dropdb -U stocka --if-exists --force $taskBancoTeste
        if ($LASTEXITCODE -ne 0) { Write-Warning "Nao foi possivel remover o banco temporario $taskBancoTeste." }
    }
    Pop-Location
}
