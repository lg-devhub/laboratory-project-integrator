param([string]$ArgumentosJvm = '-Xms32m -Xmx256m -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=64m')
$ErrorActionPreference = 'Stop'
$taskPorta = 8080
if ($env:SERVER_PORT) {
    if (-not [int]::TryParse($env:SERVER_PORT, [ref]$taskPorta) -or $taskPorta -lt 1 -or $taskPorta -gt 65535) {
        throw 'SERVER_PORT deve ser uma porta entre 1 e 65535.'
    }
}
$taskCliente = [Net.Sockets.TcpClient]::new()
$taskPortaOcupada = $false
try {
    $taskCliente.Connect('127.0.0.1', $taskPorta)
    $taskPortaOcupada = $true
} catch {
    # Nao ha um servico atendendo: segue para a inicializacao normal.
} finally { $taskCliente.Dispose() }
if ($taskPortaOcupada) {
    Write-Host "A porta $taskPorta ja esta em uso. Nenhuma segunda API foi iniciada."
    Write-Host 'Se a API STOCKA ja estiver rodando, voce pode usar o site normalmente.'
    Write-Host 'Para reinicia-la, execute .\Parar-API.ps1 e depois .\Iniciar-API.ps1.'
    Write-Host 'Se a porta pertencer a outro programa, este projeto nao vai encerra-lo.'
    return
}
Push-Location (Join-Path $PSScriptRoot 'backend')
$taskMavenAnterior = $env:MAVEN_OPTS
try {
    if (-not $env:MAVEN_OPTS) { $env:MAVEN_OPTS = '-Xms32m -Xmx192m' }
    & .\mvnw.cmd --batch-mode --no-transfer-progress spring-boot:run "-Dspring-boot.run.jvmArguments=$ArgumentosJvm"
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel executar a API. Confira a mensagem acima.' }
} finally { $env:MAVEN_OPTS = $taskMavenAnterior; Pop-Location }
