$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    # V001 e o seed sao exclusivos da primeira inicializacao. As atualizacoes sao reexecutaveis.
    foreach ($taskMigration in @('V002__cor_ambiente.sql','V003__inventario.sql','V004__edicao_auditoria_itens.sql','V005__abertura_ordens_servico.sql','V006__andamento_encerramento_os.sql','V007__manutencoes.sql')) {
        Get-Content -LiteralPath (Join-Path $PSScriptRoot "database/migrations/$taskMigration") -Raw |
            docker compose exec -T db psql -X -U stocka -d stocka -v ON_ERROR_STOP=1
        if ($LASTEXITCODE -ne 0) { throw "Falha na migracao $taskMigration. Os dados nao foram apagados." }
    }
    Write-Host 'Esquema STOCKA atualizado; dados existentes preservados.'
} finally { Pop-Location }
