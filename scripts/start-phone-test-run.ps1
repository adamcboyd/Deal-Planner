param(
    [switch]$Help,
    [switch]$RequireGemini,
    [switch]$TestGeminiLive,
    [switch]$TestGeminiImage,
    [switch]$SkipNetwork,
    [switch]$SkipBuild,
    [switch]$NoLaunch,
    [switch]$SkipSamples,
    [switch]$SkipReport,
    [switch]$WaitForPhone,
    [int]$WaitSeconds = 120,
    [string]$JavaHome = "C:\Program Files\Java\jdk-20"
)

$ErrorActionPreference = "Stop"

function Show-Usage {
    Write-Host "Deal Planner one-command phone test starter"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\start-phone-test-run.ps1"
    Write-Host "  .\scripts\start-phone-test-run.ps1 -WaitForPhone"
    Write-Host "  .\scripts\start-phone-test-run.ps1 -WaitForPhone -RequireGemini -TestGeminiLive -TestGeminiImage"
    Write-Host "  .\scripts\start-phone-test-run.ps1 -SkipBuild -NoLaunch"
    Write-Host ""
    Write-Host "What it does:"
    Write-Host "  1. Builds the current debug APK unless -SkipBuild is used."
    Write-Host "  2. Runs phone preflight with -RequirePhone against the APK that will be installed."
    Write-Host "  3. Creates deterministic TXT, PDF, PNG, pantry-label, and UPC-A barcode samples."
    Write-Host "  4. Copies those samples to the phone Downloads folder and verifies transfer."
    Write-Host "  5. Installs/launches the debug APK."
    Write-Host "  6. Creates a timestamped phone-test report."
    Write-Host "     If setup fails before that step, a failure-state report is still created unless -SkipReport is used."
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -RequireGemini  Also require a compiled real Gemini key/model before continuing."
    Write-Host "  -TestGeminiLive Make a short live Gemini API call during preflight without printing the key."
    Write-Host "  -TestGeminiImage Make a live Gemini image request using the generated pantry-label sample."
    Write-Host "  -SkipNetwork    Skip GitHub and Open Food Facts checks during preflight."
    Write-Host "  -SkipBuild      Reuse the existing debug APK after install-helper freshness checks."
    Write-Host "  -NoLaunch       Install but do not launch the app."
    Write-Host "  -SkipSamples    Do not generate or copy deterministic phone sample files."
    Write-Host "  -SkipReport     Do not create a timestamped phone-test report."
    Write-Host "  -WaitForPhone   Wait for one authorized adb device before required-phone preflight."
    Write-Host "  -WaitSeconds N  Max seconds to wait with -WaitForPhone. Default: 120"
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

function Invoke-Step {
    param(
        [string]$Label,
        [scriptblock]$Command
    )

    Write-Host ""
    Write-Host "==> $Label"
    & $Command
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

function Wait-ForAuthorizedPhone {
    param([int]$TimeoutSeconds)

    if ($TimeoutSeconds -lt 1) {
        throw "WaitSeconds must be at least 1."
    }

    if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
        throw "adb was not found on PATH. Install Android platform-tools or open from Android Studio's configured SDK."
    }

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $adbOutput = @(& adb devices)
        $authorizedDevices = @(
            $adbOutput |
                Where-Object { $_ -match "^(\S+)\s+device$" } |
                ForEach-Object { ($_.Trim() -split "\s+")[0] }
        )
        $problemDevices = @($adbOutput | Where-Object { $_ -match "^(\S+)\s+(unauthorized|offline)$" })

        if ($env:ANDROID_SERIAL) {
            if ($authorizedDevices -contains $env:ANDROID_SERIAL) {
                Write-Host "Authorized Android phone found: $env:ANDROID_SERIAL"
                return
            }

            Write-Host "Waiting for ANDROID_SERIAL=$env:ANDROID_SERIAL to be authorized..."
        } elseif ($authorizedDevices.Count -eq 1) {
            Write-Host "Authorized Android phone found: $($authorizedDevices[0])"
            return
        } elseif ($authorizedDevices.Count -gt 1) {
            throw "More than one authorized Android device found: $($authorizedDevices -join ', '). Set ANDROID_SERIAL and rerun."
        } elseif ($problemDevices.Count -gt 0) {
            Write-Host "Phone is visible but not ready. Unlock it and accept the USB debugging prompt."
        } else {
            Write-Host "Waiting for an authorized Android phone. Connect USB, choose a data-capable mode/cable, and accept USB debugging."
        }

        Start-Sleep -Seconds 3
    } while ((Get-Date) -lt $deadline)

    throw "Timed out after $TimeoutSeconds second(s) waiting for one authorized Android phone. Confirm adb devices shows a device, then rerun."
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
$script:samplesCreatedBeforePreflight = $false
$setupMode = "RequireGemini=$($RequireGemini.IsPresent); TestGeminiLive=$($TestGeminiLive.IsPresent); TestGeminiImage=$($TestGeminiImage.IsPresent); SkipNetwork=$($SkipNetwork.IsPresent); SkipBuild=$($SkipBuild.IsPresent); NoLaunch=$($NoLaunch.IsPresent); SkipSamples=$($SkipSamples.IsPresent); WaitForPhone=$($WaitForPhone.IsPresent); WaitSeconds=$WaitSeconds"

$preflightArgs = @("-RequirePhone", "-JavaHome", $JavaHome)
if ($RequireGemini) {
    $preflightArgs += "-RequireGemini"
}
if ($TestGeminiLive) {
    $preflightArgs += "-TestGeminiLive"
}
if ($TestGeminiImage) {
    $preflightArgs += "-TestGeminiImage"
}
if ($SkipNetwork) {
    $preflightArgs += "-SkipNetwork"
}

try {
    if (-not $SkipBuild) {
        Invoke-Step "Build current debug APK" {
            .\gradlew.bat testDebugUnitTest assembleDebug lintDebug
        }
    }

    if ($TestGeminiImage -and -not $SkipSamples) {
        Invoke-Helper "Create deterministic phone samples for Gemini image check" (Join-Path $PSScriptRoot "new-phone-test-samples.ps1")
        $script:samplesCreatedBeforePreflight = $true
    }

    if ($WaitForPhone) {
        Invoke-Step "Wait for authorized Android phone" {
            Wait-ForAuthorizedPhone -TimeoutSeconds $WaitSeconds
        }
    }

    Invoke-Helper "Phone preflight" (Join-Path $PSScriptRoot "phone-debug-preflight.ps1") $preflightArgs

    if (-not $SkipSamples) {
        if (-not $script:samplesCreatedBeforePreflight) {
            Invoke-Helper "Create deterministic phone samples" (Join-Path $PSScriptRoot "new-phone-test-samples.ps1")
        }
        Invoke-Helper "Copy deterministic samples to phone" (Join-Path $PSScriptRoot "send-phone-test-samples.ps1")
    }

    $installArgs = @("-JavaHome", $JavaHome, "-SkipBuild")
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
