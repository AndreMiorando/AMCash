param(
    [switch]$SkipDatabase
)

$ErrorActionPreference = "Stop"
$apiDirectory = $PSScriptRoot
$repositoryDirectory = Split-Path -Parent $apiDirectory
$environmentFile = Join-Path $apiDirectory ".env.local"
$composeFile = Join-Path $repositoryDirectory "compose.yaml"

if (-not (Test-Path -LiteralPath $environmentFile)) {
    throw "Arquivo .env.local não encontrado. Copie .env.example e preencha as configurações locais."
}

Get-Content -LiteralPath $environmentFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#")) {
        $parts = $line.Split("=", 2)
        if ($parts.Count -eq 2) {
            [Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), "Process")
        }
    }
}

if ([string]::IsNullOrWhiteSpace($env:GOOGLE_CLIENT_ID)) {
    Write-Warning "GOOGLE_CLIENT_ID ainda não foi preenchido em amcash-api/.env.local. A API iniciará, mas o login Google não funcionará."
}

if (-not $SkipDatabase) {
    docker compose --file $composeFile up --detach postgres
    if ($LASTEXITCODE -ne 0) {
        throw "Não foi possível iniciar o PostgreSQL pelo Docker Compose."
    }
}

& (Join-Path $apiDirectory "mvnw.cmd") spring-boot:run
exit $LASTEXITCODE

