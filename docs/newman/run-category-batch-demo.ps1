param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$EnvFile = "",
    [int]$Iterations = 10,
    [int]$DelayMs = 50
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$postmanDir = Join-Path $repoRoot "docs\postman"
$reportDir = Join-Path $PSScriptRoot "reports"
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"

if ([string]::IsNullOrWhiteSpace($EnvFile)) {
    $EnvFile = Join-Path $postmanDir "WHS_Local_Dev.postman_environment.json"
}

$categoryCollection = Join-Path $postmanDir "05_Category_API_Test.postman_collection.json"
$batchCollection = Join-Path $postmanDir "07_Batch_API_Test.postman_collection.json"
$performanceCollection = Join-Path $postmanDir "08_Category_Batch_Performance.postman_collection.json"
$runtimeEnv = Join-Path $reportDir "$timestamp-category-batch-runtime-env.json"

function Assert-FileExists {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Required file not found: $Path"
    }
}

function Invoke-NewmanCollection {
    param(
        [string]$Name,
        [string]$Collection,
        [string]$Environment,
        [string[]]$ExtraArgs = @()
    )

    $safeName = $Name -replace "[^A-Za-z0-9_-]", "_"
    $reportFile = Join-Path $reportDir "$timestamp-$safeName.html"
    $args = @(
        "run", $Collection,
        "-e", $Environment,
        "--env-var", "baseUrl=$BaseUrl",
        "--reporters", "cli,htmlextra",
        "--reporter-htmlextra-export", $reportFile,
        "--reporter-htmlextra-title", $Name,
        "--timeout-request", "30000"
    )
    $args += $ExtraArgs

    Write-Host ""
    Write-Host "Running $Name"
    & newman @args
    if ($LASTEXITCODE -ne 0) {
        throw "$Name failed. Report: $reportFile"
    }
    Write-Host "Report: $reportFile"
}

Write-Host "WHS Category/Batch demo runner"
Write-Host "Base URL: $BaseUrl"
Write-Host "Environment: $EnvFile"
Write-Host "Iterations: $Iterations"

if (-not (Get-Command newman -ErrorAction SilentlyContinue)) {
    throw "Newman not found. Install with: npm install -g newman newman-reporter-htmlextra"
}

New-Item -ItemType Directory -Force -Path $reportDir | Out-Null

Assert-FileExists $EnvFile
Assert-FileExists $categoryCollection
Assert-FileExists $batchCollection
Assert-FileExists $performanceCollection

Write-Host ""
Write-Host "Checking backend health..."
$health = Invoke-WebRequest -Uri "$BaseUrl/actuator/health" -UseBasicParsing -TimeoutSec 10
if ($health.StatusCode -ne 200) {
    throw "Backend health check failed with HTTP $($health.StatusCode)"
}
Write-Host "Backend is healthy"

Invoke-NewmanCollection `
    -Name "Category Functional Auto Tests" `
    -Collection $categoryCollection `
    -Environment $EnvFile

Invoke-NewmanCollection `
    -Name "Batch Functional Auto Tests" `
    -Collection $batchCollection `
    -Environment $EnvFile

Invoke-NewmanCollection `
    -Name "Category Batch Performance Setup" `
    -Collection $performanceCollection `
    -Environment $EnvFile `
    -ExtraArgs @("--folder", "00 - Setup", "--export-environment", $runtimeEnv)

Invoke-NewmanCollection `
    -Name "Category Read Performance" `
    -Collection $performanceCollection `
    -Environment $runtimeEnv `
    -ExtraArgs @("--folder", "01 - Category Read Performance", "--iteration-count", "$Iterations", "--delay-request", "$DelayMs")

Invoke-NewmanCollection `
    -Name "Batch Read Performance" `
    -Collection $performanceCollection `
    -Environment $runtimeEnv `
    -ExtraArgs @("--folder", "02 - Batch Read Performance", "--iteration-count", "$Iterations", "--delay-request", "$DelayMs")

Write-Host ""
Write-Host "Category/Batch demo tests completed."
Write-Host "Reports directory: $reportDir"
