param(
    [switch]$Help,
    [switch]$RequireGemini,
    [switch]$SkipNetwork,
    [switch]$SkipBuild,
    [switch]$NoLaunch,
    [switch]$SkipSamples,
    [switch]$SkipReport,
    [string]$JavaHome = "C:\Program Files\Java\jdk-20"
)

$ErrorActionPreference = "Stop"

function Show-Usage {
    Write-Host "Deal Planner one-command phone test starter"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\start-phone-test-run.ps1"
    Write-Host "  .\scripts\start-phone-test-run.ps1 -RequireGemini"
    Write-Host "  .\scripts\start-phone-test-run.ps1 -SkipBuild -NoLaunch"
    Write-Host ""
    Write-Host "What it does:"
    Write-Host "  1. Runs phone preflight with -RequirePhone."
    Write-Host "  2. Creates deterministic receipt/flyer TXT and PDF samples."
    Write-Host "  3. Copies those samples to the phone Downloads folder."
    Write-Host "  4. Builds/installs/launches the debug APK."
    Write-Host "  5. Creates a timestamped phone-test report."
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -RequireGemini  Also require a compiled real Gemini key/model before continuing."
    Write-Host "  -SkipNetwork    Skip GitHub and Open Food Facts checks during preflight."
    Write-Host "  -SkipBuild      Reuse the existing debug APK after install-helper freshness checks."
    Write-Host "  -NoLaunch       Install but do not launch the app."
    Write-Host "  -SkipSamples    Do not generate or copy deterministic phone sample files."
    Write-Host "  -SkipReport     Do not create a timestamped phone-test report."
    Write-Host "  -JavaHome PATH  Java home used for readiness/build checks. Default: C:\Program Files\Java\jdk-20"
    Write-Host ""
    Write-Host "Set ANDROID_SERIAL when more than one authorized Android device is connected."
}

if ($Help) {
    Show-Usage
    exit 0
}

function Invoke-Helper {
    param(
        [string]$Label,
        [string]$ScriptPath,
        [string[]]$Arguments = @()
    )

    Write-Host ""
    Write-Host "==> $Label"
    & powershell -NoProfile -ExecutionPolicy Bypass -File $ScriptPath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Label failed with exit code $LASTEXITCODE."
    }
}

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

if (-not (Test-Path ".\gradlew.bat")) {
    throw "Run this script from the Deal Planner repo, or keep it under scripts\ in that repo."
}

if (-not (Test-Path $JavaHome)) {
    throw "JavaHome does not exist: $JavaHome"
}

$env:JAVA_HOME = $JavaHome
$env:Path = "$JavaHome\bin;$env:Path"

$preflightArgs = @("-RequirePhone", "-JavaHome", $JavaHome)
if ($RequireGemini) {
    $preflightArgs += "-RequireGemini"
}
if ($SkipNetwork) {
    $preflightArgs += "-SkipNetwork"
}

Invoke-Helper "Phone preflight" (Join-Path $PSScriptRoot "phone-debug-preflight.ps1") $preflightArgs

if (-not $SkipSamples) {
    Invoke-Helper "Create deterministic phone samples" (Join-Path $PSScriptRoot "new-phone-test-samples.ps1")
    Invoke-Helper "Copy deterministic samples to phone" (Join-Path $PSScriptRoot "send-phone-test-samples.ps1")
}

$installArgs = @("-JavaHome", $JavaHome)
if ($SkipBuild) {
    $installArgs += "-SkipBuild"
}
if ($NoLaunch) {
    $installArgs += "-NoLaunch"
}

Invoke-Helper "Install Deal Planner debug APK" (Join-Path $PSScriptRoot "phone-debug-install.ps1") $installArgs

if (-not $SkipReport) {
    Invoke-Helper "Create phone-test report" (Join-Path $PSScriptRoot "new-phone-test-report.ps1")
}

Write-Host ""
Write-Host "Phone test setup complete."
Write-Host "Use PHONE_TEST_CHECKLIST_2026-07-16.md on the phone run, and capture logs with .\scripts\phone-debug-logs.ps1 if anything fails."
