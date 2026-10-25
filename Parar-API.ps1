$ErrorActionPreference = 'Stop'
$taskPorta = 8080
if ($env:SERVER_PORT) {
    if (-not [int]::TryParse($env:SERVER_PORT, [ref]$taskPorta) -or $taskPorta -lt 1 -or $taskPorta -gt 65535) {
        throw 'SERVER_PORT deve ser uma porta entre 1 e 65535.'
    }
}
$taskConexoes = @(Get-NetTCPConnection -LocalPort $taskPorta -State Listen -ErrorAction SilentlyContinue)
if ($taskConexoes.Count -eq 0) {
    Write-Host "Nao ha API escutando na porta $taskPorta."
    return
}
$taskPastaBackend = Join-Path $PSScriptRoot 'backend'
foreach ($taskProcessoId in @($taskConexoes.OwningProcess | Select-Object -Unique)) {
    $taskProcesso = Get-CimInstance Win32_Process -Filter "ProcessId = $taskProcessoId"
    $taskPai = Get-CimInstance Win32_Process -Filter "ProcessId = $($taskProcesso.ParentProcessId)"
    # So encerra a API STOCKA iniciada pelo Maven deste checkout.
    if ($taskProcesso.Name -ne 'java.exe' -or
        $taskProcesso.CommandLine -notmatch '\bbr\.com\.projetointegrador\.ApiApplication\b' -or
        $null -eq $taskPai -or $taskPai.CommandLine -notlike "*$taskPastaBackend*") {
        throw "A porta $taskPorta pertence a outro programa ou a uma API iniciada de outra forma. Encerre-a pelo terminal/IDE de origem. Nenhum processo foi encerrado."
    }
    Stop-Process -Id $taskProcessoId -ErrorAction Stop
}
Write-Host 'API STOCKA encerrada. O banco e o site nao foram parados.'
