param(
    [switch]$Help,
    [string]$Model = "",
    [string]$Prompt = "Reply with OK to confirm this Deal Planner Gemini setup works.",
    [int]$TimeoutSec = 30
)

$ErrorActionPreference = "Stop"

function Show-Usage {
    Write-Host "Deal Planner Gemini connection test"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\test-gemini-connection.ps1"
    Write-Host "  .\scripts\test-gemini-connection.ps1 -Model gemini-3.5-flash"
    Write-Host ""
    Write-Host "Reads gemini.api.key and gemini.model from local.properties, or GEMINI_API_KEY and GEMINI_MODEL from the environment."
    Write-Host "The key value is never printed."
}

function Get-LocalPropertyValue {
    param(
        [string]$Path,
        [string]$Name
    )

    if (-not (Test-Path $Path)) {
        return $null
    }

    $escapedName = [regex]::Escape($Name)
    $line = Get-Content -Path $Path | Where-Object { $_ -match "^\s*$escapedName\s*=" } | Select-Object -First 1
    if (-not $line) {
        return $null
    }

    return ($line -replace "^\s*$escapedName\s*=\s*", "").Trim()
}

function Test-RealGeminiKey {
    param([string]$Value)

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return $false
    }

    $trimmed = $Value.Trim()
    return -not (
        $trimmed.Equals("YOUR_GEMINI_API_KEY", [System.StringComparison]::OrdinalIgnoreCase) -or
        $trimmed.StartsWith("YOUR_", [System.StringComparison]::OrdinalIgnoreCase)
    )
}

function Normalize-GeminiModel {
    param([string]$Value)

    $normalized = $Value.Trim() -replace "^models/", ""
    if ([string]::IsNullOrWhiteSpace($normalized)) {
        return "gemini-3.5-flash"
    }

    return $normalized
}

function ConvertTo-StatusDetail {
    param(
        [int]$StatusCode,
        [string]$ErrorText
    )

    $detail = $null
    if (-not [string]::IsNullOrWhiteSpace($ErrorText)) {
        try {
            $parsed = $ErrorText | ConvertFrom-Json
            if ($parsed.error) {
                $parts = @()
                if ($parsed.error.status) {
                    $parts += [string]$parsed.error.status
                }
                if ($parsed.error.message) {
                    $parts += [string]$parsed.error.message
                }
                $detail = ($parts -join ": ").Trim()
            }
        } catch {
            $detail = $null
        }
    }

    if ([string]::IsNullOrWhiteSpace($detail)) {
        $detail = ($ErrorText -split "\r?\n" | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }) -join " "
    }
    if ([string]::IsNullOrWhiteSpace($detail)) {
        $detail = "unknown error"
    }

    if ($detail.Length -gt 180) {
        $detail = $detail.Substring(0, 177).TrimEnd() + "..."
    }

    return "HTTP $StatusCode $detail"
}

function Get-WebExceptionBody {
    param($ErrorRecord)

    if ($ErrorRecord.ErrorDetails -and -not [string]::IsNullOrWhiteSpace($ErrorRecord.ErrorDetails.Message)) {
        return $ErrorRecord.ErrorDetails.Message
    }

    $response = $ErrorRecord.Exception.Response
    if ($null -eq $response) {
        return ""
    }

    try {
        $stream = $response.GetResponseStream()
        if ($null -eq $stream) {
            return ""
        }

        $reader = [System.IO.StreamReader]::new($stream)
        try {
            return $reader.ReadToEnd()
        } finally {
            $reader.Dispose()
        }
    } catch {
        return ""
    }
}

function Get-WebExceptionStatusCode {
    param($ErrorRecord)

    $response = $ErrorRecord.Exception.Response
    if ($response -and $response.StatusCode) {
        return [int]$response.StatusCode
    }

    return 0
}

if ($Help) {
    Show-Usage
    exit 0
}

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$localPropertiesPath = Join-Path $repoRoot "local.properties"
$localGeminiKey = Get-LocalPropertyValue $localPropertiesPath "gemini.api.key"
$envGeminiKey = $env:GEMINI_API_KEY
$localGeminiModel = Get-LocalPropertyValue $localPropertiesPath "gemini.model"
$envGeminiModel = $env:GEMINI_MODEL

$keySource = ""
$apiKey = ""
if (Test-RealGeminiKey $localGeminiKey) {
    $apiKey = $localGeminiKey.Trim()
    $keySource = "local.properties"
} elseif (Test-RealGeminiKey $envGeminiKey) {
    $apiKey = $envGeminiKey.Trim()
    $keySource = "GEMINI_API_KEY"
}

$modelSource = if (-not [string]::IsNullOrWhiteSpace($Model)) {
    $Model
} elseif (-not [string]::IsNullOrWhiteSpace($localGeminiModel)) {
    $localGeminiModel
} elseif (-not [string]::IsNullOrWhiteSpace($envGeminiModel)) {
    $envGeminiModel
} else {
    "gemini-3.5-flash"
}
$modelName = Normalize-GeminiModel $modelSource

Write-Host "Deal Planner Gemini connection test"
Write-Host "Repo: $repoRoot"
Write-Host "Model: $modelName"

if ([string]::IsNullOrWhiteSpace($apiKey)) {
    Write-Host "[FAIL] No non-placeholder Gemini key found. Add gemini.api.key to local.properties or set GEMINI_API_KEY."
    exit 1
}

Write-Host "Key source: $keySource (value not printed)"

$body = @{
    contents = @(
        @{
            parts = @(
                @{
                    text = $Prompt
                }
            )
        }
    )
    generationConfig = @{
        maxOutputTokens = 16
    }
} | ConvertTo-Json -Depth 10

$uri = "https://generativelanguage.googleapis.com/v1beta/models/$modelName`:generateContent"
$headers = @{
    "x-goog-api-key" = $apiKey
}

try {
    $response = Invoke-RestMethod -Method Post -Uri $uri -Headers $headers -ContentType "application/json" -Body $body -TimeoutSec $TimeoutSec
    $candidates = @($response.candidates)
    $parts = if ($candidates.Count -gt 0 -and $candidates[0].content) {
        @($candidates[0].content.parts)
    } else {
        @()
    }
    $answer = (
        $parts |
            ForEach-Object { $_.text } |
            Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
    ) -join "`n"

    if ([string]::IsNullOrWhiteSpace($answer)) {
        Write-Host "[FAIL] Gemini responded, but returned an empty test response."
        exit 1
    }

    Write-Host "[OK] Gemini connection OK using $modelName."
    exit 0
} catch {
    $statusCode = Get-WebExceptionStatusCode $_
    $errorText = Get-WebExceptionBody $_
    if ($statusCode -gt 0) {
        Write-Host "[FAIL] Gemini request failed: $(ConvertTo-StatusDetail -StatusCode $statusCode -ErrorText $errorText)"
    } else {
        Write-Host "[FAIL] Gemini request failed: $($_.Exception.Message)"
    }
    exit 1
}
