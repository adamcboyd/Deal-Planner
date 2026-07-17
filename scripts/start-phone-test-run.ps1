param(
    [switch]$Help,
    [switch]$RequireGemini,
    [switch]$TestGeminiLive,
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
    Write-Host "  .\scripts\start-phone-test-run.ps1 -RequireGemini -TestGeminiLive"
    Write-Host "  .\scripts\start-phone-test-run.ps1 -SkipBuild -NoLaunch"
    Write-Host ""
    Write-Host "What it does:"
    Write-Host "  1. Runs phone preflight with -RequirePhone."
    Write-Host "  2. Creates deterministic TXT, PDF, PNG, pantry-label, and UPC-A barcode samples."
    Write-Host "  3. Copies those samples to the phone Downloads folder and verifies transfer."
    Write-Host "  4. Builds/installs/launches the debug APK."
    Write-Host "  5. Creates a timestamped phone-test report."
    Write-Host "     If setup fails before that step, a failure-state report is still created unless -SkipReport is used."
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -RequireGemini  Also require a compiled real Gemini key/model before continuing."
    Write-Host "  -TestGeminiLive Make a short live Gemini API call during preflight without printing the key."
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

function Invoke-PhoneTestReport {
    param(
        [string]$Label,
        [string]$SetupStatus = "Not recorded",
        [string]$SetupFailure = "",
        [string]$SetupMode = ""
    )

    $reportArgs = @("-SetupStatus", $SetupStatus)
    if (-not [string]::IsNullOrWhiteSpace($SetupFailure)) {
        $reportArgs += @("-SetupFailure", $SetupFailure)
    }
    if (-not [string]::IsNullOrWhiteSpace($SetupMode)) {
        $reportArgs += @("-SetupMode", $SetupMode)
    }

    Invoke-Helper $Label (Join-Path $PSScriptRoot "new-phone-test-report.ps1") $reportArgs
    $script:reportCreated = $true
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
$script:reportCreated = $false
$setupMode = "RequireGemini=$($RequireGemini.IsPresent); TestGeminiLive=$($TestGeminiLive.IsPresent); SkipNetwork=$($SkipNetwork.IsPresent); SkipBuild=$($SkipBuild.IsPresent); NoLaunch=$($NoLaunch.IsPresent); SkipSamples=$($SkipSamples.IsPresent)"

$preflightArgs = @("-RequirePhone", "-JavaHome", $JavaHome)
if ($RequireGemini) {
    $preflightArgs += "-RequireGemini"
}
if ($TestGeminiLive) {
    $preflightArgs += "-TestGeminiLive"
}
if ($SkipNetwork) {
    $preflightArgs += "-SkipNetwork"
}

try {
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
        Invoke-PhoneTestReport -Label "Create phone-test report" -SetupStatus "Completed" -SetupMode $setupMode
    }

    Write-Host ""
    Write-Host "Phone test setup complete."
    Write-Host "Use PHONE_TEST_CHECKLIST_2026-07-16.md on the phone run, and capture logs with .\scripts\phone-debug-logs.ps1 if anything fails."
} catch {
    $failureMessage = $_.Exception.Message
    if (-not $SkipReport -and -not $script:reportCreated) {
        Write-Warning "Phone test setup stopped before completion: $failureMessage"
        Write-Warning "Creating a phone-test report with the current failure state."
        try {
            Invoke-PhoneTestReport -Label "Create phone-test report after setup failure" -SetupStatus "Failed" -SetupFailure $failureMessage -SetupMode $setupMode
        } catch {
            Write-Warning "Could not create failure-state phone-test report: $($_.Exception.Message)"
        }
    }
    throw
}
