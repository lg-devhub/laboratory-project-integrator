$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot 'backend')
$taskMavenAnterior = $env:MAVEN_OPTS
try {
    if (-not $env:MAVEN_OPTS) { $env:MAVEN_OPTS = '-Xms32m -Xmx192m' }
    & .\mvnw.cmd --batch-mode --no-transfer-progress test '-Dstocka.testesLocais=true' '-DargLine=-Xms32m -Xmx256m -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=64m'
    if ($LASTEXITCODE -ne 0) { throw 'Os testes do backend falharam. Confira as mensagens acima.' }
} finally { $env:MAVEN_OPTS = $taskMavenAnterior; Pop-Location }
& (Join-Path $PSScriptRoot 'Testar-Inventario.ps1')
