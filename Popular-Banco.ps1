param(
    [string]$Login = 'admin@stocka.local',
    [Security.SecureString]$Senha,
    [string]$EnderecoApi = 'http://localhost:8080'
)
$ErrorActionPreference = 'Stop'
$taskUri = [Uri]$EnderecoApi
if ($taskUri.Scheme -ne 'http' -or $taskUri.Host -notin @('localhost','127.0.0.1') -or $taskUri.AbsolutePath -ne '/') {
    throw 'Este script aceita somente a API HTTP local.'
}
$taskDbUrl = $env:DB_URL
if (-not $taskDbUrl) {
    $taskLinhaDb = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'backend/.env') -Encoding UTF8 |
        Where-Object { $_ -match '^DB_URL=' } | Select-Object -First 1
    $taskDbUrl = ($taskLinhaDb -replace '^DB_URL=','').Trim()
}
if ($taskDbUrl -notmatch '^jdbc:postgresql://(localhost|127\.0\.0\.1):15432/stocka(\?|$)') {
    throw 'Use somente o banco stocka local para estes dados de demonstracao.'
}
$EnderecoApi = $EnderecoApi.TrimEnd('/')
$taskHeaders = @{ 'X-STOCKA-Request'='true' }
$taskSessao = $null
$taskSenha = if ($Senha) { [Net.NetworkCredential]::new('', $Senha).Password } else { 'Stocka123' }
$taskCriados = 0
$taskItensCriados = 0
$taskDadosDemo = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'database/seed/demonstracao.json') -Raw -Encoding UTF8 | ConvertFrom-Json
function Enviar-JsonDemo($Caminho, $Metodo, $Dados) {
    $taskJson = $Dados | ConvertTo-Json -Depth 5
    Invoke-RestMethod "$EnderecoApi$Caminho" -Method $Metodo -Headers $taskHeaders -ContentType 'application/json' -Body ([Text.Encoding]::UTF8.GetBytes($taskJson)) -WebSession $taskSessao
}
try {
    $taskCorpo = @{login=$Login;senha=$taskSenha} | ConvertTo-Json
    $taskConta = Invoke-RestMethod "$EnderecoApi/api/auth/login" -Method Post -Headers $taskHeaders -ContentType 'application/json' -Body ([Text.Encoding]::UTF8.GetBytes($taskCorpo)) -SessionVariable taskSessao
    if ($taskConta.perfil -ne 'ADMINISTRADOR') { throw 'A populacao exige uma conta administradora.' }
    $taskAmbientes = Invoke-RestMethod "$EnderecoApi/api/ambientes" -WebSession $taskSessao
    $taskMapa = @{}
    foreach ($taskDef in $taskDadosDemo.ambientes) {
        $taskExistente = @($taskAmbientes | Where-Object { $_.nome -eq $taskDef.nome })
        if ($taskExistente.Count -gt 0) {
            if ($taskExistente[0].tipo -ne $taskDef.tipo -or $taskExistente[0].descricao -notlike 'Demonstracao STOCKA:*') {
                throw "O nome $($taskDef.nome) ja existe com outra finalidade; nenhum registro existente foi alterado."
            }
            $taskMapa[$taskDef.chave] = $taskExistente[0]
            continue
        }
        $taskNovo = Enviar-JsonDemo '/api/ambientes' 'Post' @{
            nome=$taskDef.nome;tipo=$taskDef.tipo;cor=$taskDef.cor
            descricao='Demonstracao STOCKA: ambiente ficticio para validar o inventario. Nao representa dados reais da empresa.'
        }
        $taskMapa[$taskDef.chave] = $taskNovo
        $taskCriados++
    }
    # Preserva atribuicoes existentes. Acrescenta o operador somente nos ambientes DEMO.
    foreach ($taskAmbiente in $taskMapa.Values) {
        $taskAcessos = Invoke-RestMethod "$EnderecoApi/api/ambientes/$($taskAmbiente.id)/acessos" -WebSession $taskSessao
        $taskOperador = @($taskAcessos | Where-Object { $_.login -eq 'operador@stocka.local' -and $_.ativo })
        if ($taskOperador.Count -eq 0 -or $taskOperador[0].atribuido) { continue }
        $taskIds = @($taskAcessos | Where-Object atribuido | ForEach-Object { [long]$_.id }) + @([long]$taskOperador[0].id)
        Enviar-JsonDemo "/api/ambientes/$($taskAmbiente.id)/acessos" 'Put' @{usuarios=@($taskIds | Select-Object -Unique)} | Out-Null
    }
    foreach ($taskExemplo in $taskDadosDemo.itens) {
        $taskAmbienteId = [long]$taskMapa[$taskExemplo.ambiente].id
        $taskItens = Invoke-RestMethod "$EnderecoApi/api/ambientes/$taskAmbienteId/itens" -WebSession $taskSessao
        $taskExistentes = @($taskItens | Where-Object { $_.nome -eq $taskExemplo.nome -or
            ($taskExemplo.numeroPatrimonio -and $_.numeroPatrimonio -eq $taskExemplo.numeroPatrimonio) })
        if ($taskExistentes.Count -gt 0) { continue }
        $taskEquipamento = $null -ne $taskExemplo.numeroPatrimonio
        $taskResponsavel = if ($taskEquipamento) { [long]$taskMapa[$taskExemplo.responsavel].id } else { $null }
        $taskForma = if ($taskExemplo.quantidade -eq 1) { 'INDIVIDUAL' } else { 'QUANTIDADE' }
        Enviar-JsonDemo "/api/ambientes/$taskAmbienteId/itens" 'Post' @{
            nome=$taskExemplo.nome;categoria=$taskExemplo.categoria;status=$taskExemplo.status
            equipamento=$taskEquipamento;numeroPatrimonio=$taskExemplo.numeroPatrimonio;setorResponsavelId=$taskResponsavel
            quantidade=[int]$taskExemplo.quantidade;formaRegistro=$taskForma;localizacao=$taskExemplo.localizacao
            descricao='Dado ficticio de demonstracao STOCKA para validar cadastro, edicao e pesquisa.'
        } | Out-Null
        $taskItensCriados++
    }
    Write-Host "Demonstracao pronta. Ambientes criados: $taskCriados. Itens criados: $taskItensCriados."
    Write-Host 'Somente ambientes DEMO receberam atribuicao adicional para operador@stocka.local.'
    Write-Host 'Os cadastros foram feitos pela API e geraram auditoria. Dados anteriores foram preservados.'
} finally {
    $taskSenha = $null
    $taskCorpo = $null
    if ($null -ne $taskSessao) {
        try { Invoke-RestMethod "$EnderecoApi/api/auth/logout" -Method Post -Headers $taskHeaders -WebSession $taskSessao | Out-Null } catch { }
    }
}
