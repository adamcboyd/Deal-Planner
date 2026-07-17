param(
    [switch]$Help,
    [switch]$VerifyOnly,
    [string]$SamplesDir = "",
    [string]$OutputDir = "phone-test-samples"
)

$ErrorActionPreference = "Stop"

function Show-Usage {
    Write-Host "Deal Planner phone test sample generator"
    Write-Host ""
    Write-Host "Usage:"
    Write-Host "  .\scripts\new-phone-test-samples.ps1"
    Write-Host "  .\scripts\new-phone-test-samples.ps1 -VerifyOnly"
    Write-Host "  .\scripts\new-phone-test-samples.ps1 -VerifyOnly -SamplesDir phone-test-samples\20260716-103940"
    Write-Host "  .\scripts\new-phone-test-samples.ps1 -OutputDir phone-test-samples"
    Write-Host ""
    Write-Host "Creates ignored timestamped TXT, PDF, PNG, pantry-label, and UPC-A barcode sample files from bundled demo assets."
    Write-Host "The pantry-label sample covers hyphenated sizes, punctuated label cues, slash dates, and two-digit dash dates."
    Write-Host "Also verifies required files and writes SAMPLE_MANIFEST.md with byte counts and SHA-256 hashes."
}

if ($Help) {
    Show-Usage
    exit 0
}

function Escape-PdfText {
    param([string]$Value)

    return $Value.Replace("\", "\\").Replace("(", "\(").Replace(")", "\)")
}

function Split-DisplayLine {
    param(
        [string]$Line,
        [int]$MaxChars
    )

    $clean = $Line.Replace("`t", "    ")
    if ($clean.Length -le $MaxChars) {
        return @($clean)
    }

    $chunks = New-Object System.Collections.Generic.List[string]
    $remaining = $clean
    while ($remaining.Length -gt $MaxChars) {
        $breakAt = $remaining.LastIndexOf(" ", [Math]::Min($MaxChars, $remaining.Length - 1))
        if ($breakAt -lt 24) {
            $breakAt = $MaxChars
        }
        $chunks.Add($remaining.Substring(0, $breakAt).TrimEnd()) | Out-Null
        $remaining = $remaining.Substring($breakAt).TrimStart()
    }

    if ($remaining.Length -gt 0) {
        $chunks.Add($remaining) | Out-Null
    }

    return @($chunks)
}

function New-SimplePdf {
    param(
        [string]$SourceTextPath,
        [string]$OutputPath,
        [string]$Title
    )

    $rawLines = Get-Content -LiteralPath $SourceTextPath
    $displayLines = New-Object System.Collections.Generic.List[string]
    foreach ($line in $rawLines) {
        foreach ($chunk in (Split-DisplayLine -Line $line -MaxChars 86)) {
            $displayLines.Add($chunk) | Out-Null
        }
    }

    $maxLinesPerPage = 54
    $pages = New-Object System.Collections.Generic.List[object]
    for ($start = 0; $start -lt $displayLines.Count; $start += $maxLinesPerPage) {
        $count = [Math]::Min($maxLinesPerPage, $displayLines.Count - $start)
        $pages.Add(@($displayLines | Select-Object -Skip $start -First $count)) | Out-Null
    }
    if ($pages.Count -eq 0) {
        $pages.Add(@("")) | Out-Null
    }

    $objects = New-Object System.Collections.Generic.List[string]
    $objects.Add("<< /Type /Catalog /Pages 2 0 R >>") | Out-Null
    $objects.Add("__PAGES__") | Out-Null
    $objects.Add("<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>") | Out-Null

    $pageObjectIds = New-Object System.Collections.Generic.List[int]
    foreach ($pageLines in $pages) {
        $pageObjectId = $objects.Count + 1
        $contentObjectId = $objects.Count + 2
        $pageObjectIds.Add($pageObjectId) | Out-Null

        $contentBuilder = [System.Text.StringBuilder]::new()
        [void]$contentBuilder.AppendLine("BT")
        [void]$contentBuilder.AppendLine("/F1 10 Tf")
        [void]$contentBuilder.AppendLine("36 756 Td")
        [void]$contentBuilder.AppendLine("12 TL")
        [void]$contentBuilder.AppendLine(("({0}) Tj" -f (Escape-PdfText $Title)))
        [void]$contentBuilder.AppendLine("T*")
        [void]$contentBuilder.AppendLine("T*")
        foreach ($line in $pageLines) {
            if ([string]::IsNullOrWhiteSpace($line)) {
                [void]$contentBuilder.AppendLine("T*")
            } else {
                [void]$contentBuilder.AppendLine(("({0}) Tj" -f (Escape-PdfText $line)))
                [void]$contentBuilder.AppendLine("T*")
            }
        }
        [void]$contentBuilder.AppendLine("ET")

        $content = $contentBuilder.ToString()
        $contentByteLength = [System.Text.Encoding]::ASCII.GetByteCount($content)
        $objects.Add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 3 0 R >> >> /Contents $contentObjectId 0 R >>") | Out-Null
        $objects.Add("<< /Length $contentByteLength >>`nstream`n$content`nendstream") | Out-Null
    }

    $kids = ($pageObjectIds | ForEach-Object { "$_ 0 R" }) -join " "
    $objects[1] = "<< /Type /Pages /Kids [ $kids ] /Count $($pageObjectIds.Count) >>"

    $encoding = [System.Text.Encoding]::ASCII
    $builder = [System.Text.StringBuilder]::new()
    [void]$builder.Append("%PDF-1.4`n% Deal Planner phone test sample`n")

    $offsets = New-Object System.Collections.Generic.List[int]
    for ($index = 0; $index -lt $objects.Count; $index++) {
        $offsets.Add($encoding.GetByteCount($builder.ToString())) | Out-Null
        [void]$builder.Append(("{0} 0 obj`n{1}`nendobj`n" -f ($index + 1), $objects[$index]))
    }

    $xrefOffset = $encoding.GetByteCount($builder.ToString())
    [void]$builder.AppendLine("xref")
    [void]$builder.AppendLine(("0 {0}" -f ($objects.Count + 1)))
    [void]$builder.AppendLine("0000000000 65535 f ")
    foreach ($offset in $offsets) {
        [void]$builder.AppendLine(("{0:D10} 00000 n " -f $offset))
    }
    [void]$builder.AppendLine("trailer")
    [void]$builder.AppendLine(("<< /Size {0} /Root 1 0 R >>" -f ($objects.Count + 1)))
    [void]$builder.AppendLine("startxref")
    [void]$builder.AppendLine($xrefOffset.ToString())
    [void]$builder.AppendLine("%%EOF")

    [System.IO.File]::WriteAllBytes($OutputPath, $encoding.GetBytes($builder.ToString()))
}

function New-TextImage {
    param(
        [string[]]$Lines,
        [string]$OutputPath,
        [string]$Title
    )

    Add-Type -AssemblyName System.Drawing

    $displayLines = New-Object System.Collections.Generic.List[string]
    foreach ($line in $Lines) {
        foreach ($chunk in (Split-DisplayLine -Line $line -MaxChars 52)) {
            $displayLines.Add($chunk) | Out-Null
        }
    }
    if ($displayLines.Count -eq 0) {
        $displayLines.Add("") | Out-Null
    }

    $width = 1400
    $margin = 64
    $titleHeight = 72
    $lineHeight = 38
    $height = [Math]::Max(720, ($margin * 2) + $titleHeight + ($displayLines.Count * $lineHeight) + 48)

    $bitmap = [System.Drawing.Bitmap]::new($width, $height)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.Clear([System.Drawing.Color]::White)
        $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit

        $titleFont = [System.Drawing.Font]::new("Arial", 30, [System.Drawing.FontStyle]::Bold)
        $bodyFont = [System.Drawing.Font]::new("Consolas", 24, [System.Drawing.FontStyle]::Regular)
        try {
            $blackBrush = [System.Drawing.Brushes]::Black
            $grayPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(220, 220, 220), 2)
            try {
                $graphics.DrawRectangle($grayPen, 24, 24, $width - 48, $height - 48)
                $graphics.DrawString($Title, $titleFont, $blackBrush, $margin, $margin)

                $y = $margin + $titleHeight
                foreach ($line in $displayLines) {
                    $graphics.DrawString($line, $bodyFont, $blackBrush, $margin, $y)
                    $y += $lineHeight
                }
            } finally {
                $grayPen.Dispose()
            }
        } finally {
            $titleFont.Dispose()
            $bodyFont.Dispose()
        }

        $bitmap.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $graphics.Dispose()
        $bitmap.Dispose()
    }
}

function Test-UpcACheckDigit {
    param([string]$Barcode)

    if ($Barcode -notmatch "^\d{12}$") {
        return $false
    }

    $oddSum = 0
    for ($index = 0; $index -lt 11; $index += 2) {
        $oddSum += [int]::Parse($Barcode.Substring($index, 1))
    }

    $evenSum = 0
    for ($index = 1; $index -lt 11; $index += 2) {
        $evenSum += [int]::Parse($Barcode.Substring($index, 1))
    }

    $expectedCheckDigit = (10 - ((($oddSum * 3) + $evenSum) % 10)) % 10
    return $expectedCheckDigit -eq [int]::Parse($Barcode.Substring(11, 1))
}

function New-UpcABarcodeImage {
    param(
        [string]$Barcode,
        [string]$OutputPath,
        [string]$Title
    )

    if (-not (Test-UpcACheckDigit $Barcode)) {
        throw "UPC-A barcode must be 12 digits with a valid check digit: $Barcode"
    }

    Add-Type -AssemblyName System.Drawing

    $leftPatterns = @{
        "0" = "0001101"
        "1" = "0011001"
        "2" = "0010011"
        "3" = "0111101"
        "4" = "0100011"
        "5" = "0110001"
        "6" = "0101111"
        "7" = "0111011"
        "8" = "0110111"
        "9" = "0001011"
    }
    $rightPatterns = @{
        "0" = "1110010"
        "1" = "1100110"
        "2" = "1101100"
        "3" = "1000010"
        "4" = "1011100"
        "5" = "1001110"
        "6" = "1010000"
        "7" = "1000100"
        "8" = "1001000"
        "9" = "1110100"
    }

    $pattern = "101"
    for ($index = 0; $index -lt 6; $index++) {
        $pattern += $leftPatterns[$Barcode.Substring($index, 1)]
    }
    $pattern += "01010"
    for ($index = 6; $index -lt 12; $index++) {
        $pattern += $rightPatterns[$Barcode.Substring($index, 1)]
    }
    $pattern += "101"

    $moduleWidth = 5
    $quietModules = 10
    $barcodeWidth = ($pattern.Length + ($quietModules * 2)) * $moduleWidth
    $width = [Math]::Max(820, $barcodeWidth + 120)
    $height = 620
    $barTop = 150
    $barHeight = 310
    $left = [int](($width - $barcodeWidth) / 2)

    $bitmap = [System.Drawing.Bitmap]::new($width, $height)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.Clear([System.Drawing.Color]::White)
        $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit

        $titleFont = [System.Drawing.Font]::new("Arial", 28, [System.Drawing.FontStyle]::Bold)
        $labelFont = [System.Drawing.Font]::new("Consolas", 32, [System.Drawing.FontStyle]::Bold)
        $hintFont = [System.Drawing.Font]::new("Arial", 18, [System.Drawing.FontStyle]::Regular)
        try {
            $blackBrush = [System.Drawing.Brushes]::Black
            $grayPen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(220, 220, 220), 2)
            try {
                $graphics.DrawRectangle($grayPen, 24, 24, $width - 48, $height - 48)
                $graphics.DrawString($Title, $titleFont, $blackBrush, 56, 56)

                for ($index = 0; $index -lt $pattern.Length; $index++) {
                    if ($pattern.Substring($index, 1) -eq "1") {
                        $x = $left + (($quietModules + $index) * $moduleWidth)
                        $graphics.FillRectangle($blackBrush, $x, $barTop, $moduleWidth, $barHeight)
                    }
                }

                $graphics.DrawString($Barcode, $labelFont, $blackBrush, $left + 92, $barTop + $barHeight + 24)
                $graphics.DrawString("Display on another screen or print, then scan with the phone camera.", $hintFont, $blackBrush, 56, $height - 84)
            } finally {
                $grayPen.Dispose()
            }
        } finally {
            $titleFont.Dispose()
            $labelFont.Dispose()
            $hintFont.Dispose()
        }

        $bitmap.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $graphics.Dispose()
        $bitmap.Dispose()
    }
}

function Get-LatestSampleDir {
    param([string]$Root)

    if (-not (Test-Path $Root)) {
        return $null
    }

    return Get-ChildItem -LiteralPath $Root -Directory |
        Sort-Object Name -Descending |
        Select-Object -First 1
}

function Resolve-SampleDir {
    param(
        [string]$Root,
        [string]$Path
    )

    if ([string]::IsNullOrWhiteSpace($Path)) {
        return $null
    }

    if ([System.IO.Path]::IsPathRooted($Path)) {
        return $Path
    }

    return Join-Path $Root $Path
}

function Test-PdfFile {
    param([string]$Path)

    $bytes = [System.IO.File]::ReadAllBytes($Path)
    if ($bytes.Length -lt 12) {
        return $false
    }

    $header = [System.Text.Encoding]::ASCII.GetString($bytes, 0, [Math]::Min(8, $bytes.Length))
    $tailLength = [Math]::Min(256, $bytes.Length)
    $tail = [System.Text.Encoding]::ASCII.GetString($bytes, $bytes.Length - $tailLength, $tailLength)
    return $header.StartsWith("%PDF-") -and $tail.Contains("%%EOF")
}

function Test-PngFile {
    param([string]$Path)

    $bytes = [System.IO.File]::ReadAllBytes($Path)
    if ($bytes.Length -lt 8) {
        return $false
    }

    $signature = ($bytes[0..7] | ForEach-Object { $_.ToString("X2") }) -join ""
    return $signature -eq "89504E470D0A1A0A"
}

function Assert-SampleFolder {
    param([string]$Path)

    if ([string]::IsNullOrWhiteSpace($Path) -or -not (Test-Path $Path)) {
        throw "Phone test sample folder was not found. Run .\scripts\new-phone-test-samples.ps1 first."
    }

    $requiredFiles = @(
        "deal-planner-demo-receipt.txt",
        "deal-planner-demo-receipt.pdf",
        "deal-planner-demo-receipt.png",
        "deal-planner-demo-flyer.txt",
        "deal-planner-demo-flyer.pdf",
        "deal-planner-demo-flyer.png",
        "deal-planner-demo-pantry-label.txt",
        "deal-planner-demo-pantry-label.png",
        "deal-planner-demo-upc-a.txt",
        "deal-planner-demo-upc-a.png",
        "README.md"
    )

    foreach ($fileName in $requiredFiles) {
        $fullPath = Join-Path $Path $fileName
        if (-not (Test-Path $fullPath)) {
            throw "Sample folder is missing $fileName."
        }
    }

    $pantryTextPath = Join-Path $Path "deal-planner-demo-pantry-label.txt"
    $pantryText = Get-Content -Raw -LiteralPath $pantryTextPath
    $requiredPantryText = @("16-ounce", "12-count", "net wt:", "best by:", "12/31/2026", "12-31-26")
    foreach ($expectedText in $requiredPantryText) {
        if (-not $pantryText.Contains($expectedText)) {
            throw "Pantry label sample is missing expected OCR coverage text: $expectedText"
        }
    }

    foreach ($pdfName in @("deal-planner-demo-receipt.pdf", "deal-planner-demo-flyer.pdf")) {
        $pdfPath = Join-Path $Path $pdfName
        if (-not (Test-PdfFile $pdfPath)) {
            throw "$pdfName is not a valid generated PDF sample."
        }
    }

    foreach ($pngName in @("deal-planner-demo-receipt.png", "deal-planner-demo-flyer.png", "deal-planner-demo-pantry-label.png", "deal-planner-demo-upc-a.png")) {
        $pngPath = Join-Path $Path $pngName
        if (-not (Test-PngFile $pngPath)) {
            throw "$pngName is not a valid generated PNG sample."
        }
    }
}

function Write-SampleManifest {
    param([string]$Path)

    $manifestPath = Join-Path $Path "SAMPLE_MANIFEST.md"
    $files = Get-ChildItem -LiteralPath $Path -File |
        Where-Object { $_.Name -ne "SAMPLE_MANIFEST.md" } |
        Sort-Object Name

    $rows = New-Object System.Collections.Generic.List[string]
    $rows.Add("# Deal Planner Phone Test Sample Manifest") | Out-Null
    $rows.Add("") | Out-Null
    $rows.Add("- Folder: $Path") | Out-Null
    $rows.Add("- Generated/verified: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss K")") | Out-Null
    $rows.Add("- Purpose: deterministic sample files for pasted text, picker image/PDF, pantry-label OCR, and UPC-A barcode phone checks.") | Out-Null
    $rows.Add("") | Out-Null
    $rows.Add("| File | Bytes | SHA-256 |") | Out-Null
    $rows.Add("| --- | ---: | --- |") | Out-Null

    foreach ($file in $files) {
        $hash = Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256
        $rows.Add("| $($file.Name) | $($file.Length) | $($hash.Hash) |") | Out-Null
    }

    Set-Content -LiteralPath $manifestPath -Value $rows -Encoding UTF8
    return $manifestPath
}

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$assetsRoot = Join-Path $repoRoot "app\src\main\assets"
$receiptAsset = Join-Path $assetsRoot "demo_receipt.txt"
$flyerAsset = Join-Path $assetsRoot "demo_flyer.txt"
$pantryAsset = Join-Path $assetsRoot "demo_pantry_labels.txt"

if (-not (Test-Path $receiptAsset)) {
    throw "Missing bundled demo receipt asset: $receiptAsset"
}
if (-not (Test-Path $flyerAsset)) {
    throw "Missing bundled demo flyer asset: $flyerAsset"
}
if (-not (Test-Path $pantryAsset)) {
    throw "Missing bundled demo pantry-label asset: $pantryAsset"
}

if ([System.IO.Path]::IsPathRooted($OutputDir)) {
    $outputRoot = $OutputDir
} else {
    $outputRoot = Join-Path $repoRoot $OutputDir
}

if ($VerifyOnly) {
    $sampleDir = Resolve-SampleDir $repoRoot $SamplesDir
    if (-not $sampleDir) {
        $latest = Get-LatestSampleDir $outputRoot
        if (-not $latest) {
            throw "No generated sample folder found under $outputRoot. Run .\scripts\new-phone-test-samples.ps1 first."
        }
        $sampleDir = $latest.FullName
    }

    Assert-SampleFolder $sampleDir
    $manifestPath = Write-SampleManifest $sampleDir
    Write-Host "Verified phone test samples:"
    Write-Host $sampleDir
    Write-Host "Manifest:"
    Write-Host $manifestPath
    Get-ChildItem -LiteralPath $sampleDir | Select-Object Name, Length
    exit 0
}

$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$sessionDir = Join-Path $outputRoot $stamp
New-Item -ItemType Directory -Force -Path $sessionDir | Out-Null

$receiptText = Join-Path $sessionDir "deal-planner-demo-receipt.txt"
$flyerText = Join-Path $sessionDir "deal-planner-demo-flyer.txt"
$pantryText = Join-Path $sessionDir "deal-planner-demo-pantry-label.txt"
$barcodeText = Join-Path $sessionDir "deal-planner-demo-upc-a.txt"
$receiptPdf = Join-Path $sessionDir "deal-planner-demo-receipt.pdf"
$flyerPdf = Join-Path $sessionDir "deal-planner-demo-flyer.pdf"
$receiptImage = Join-Path $sessionDir "deal-planner-demo-receipt.png"
$flyerImage = Join-Path $sessionDir "deal-planner-demo-flyer.png"
$pantryImage = Join-Path $sessionDir "deal-planner-demo-pantry-label.png"
$barcodeImage = Join-Path $sessionDir "deal-planner-demo-upc-a.png"
$readmePath = Join-Path $sessionDir "README.md"
$manifestPath = Join-Path $sessionDir "SAMPLE_MANIFEST.md"

Copy-Item -LiteralPath $receiptAsset -Destination $receiptText -Force
Copy-Item -LiteralPath $flyerAsset -Destination $flyerText -Force
Copy-Item -LiteralPath $pantryAsset -Destination $pantryText -Force
$pantryLines = Get-Content -LiteralPath $pantryText
$barcodeValue = "012345678905"
$barcodeLines = @(
    "UPC-A: $barcodeValue",
    "Use this code for Pantry -> Barcode / UPC -> Add Code.",
    "Display or print deal-planner-demo-upc-a.png on another screen for Pantry -> Scan."
)
Set-Content -LiteralPath $barcodeText -Value $barcodeLines -Encoding UTF8
New-SimplePdf -SourceTextPath $receiptAsset -OutputPath $receiptPdf -Title "Deal Planner Demo Receipt"
New-SimplePdf -SourceTextPath $flyerAsset -OutputPath $flyerPdf -Title "Deal Planner Demo Flyer"
New-TextImage -Lines (Get-Content -LiteralPath $receiptAsset) -OutputPath $receiptImage -Title "Deal Planner Demo Receipt"
New-TextImage -Lines (Get-Content -LiteralPath $flyerAsset) -OutputPath $flyerImage -Title "Deal Planner Demo Flyer"
New-TextImage -Lines $pantryLines -OutputPath $pantryImage -Title "Deal Planner Demo Pantry Labels"
New-UpcABarcodeImage -Barcode $barcodeValue -OutputPath $barcodeImage -Title "Deal Planner Demo UPC-A"

$readme = @"
# Deal Planner Phone Test Samples - $stamp

Copy this folder to the Android phone or upload it to a location the phone can open.

- deal-planner-demo-receipt.txt: paste into Receipts -> Paste receipt OCR text.
- deal-planner-demo-receipt.pdf: choose from Receipts -> PDF.
- deal-planner-demo-receipt.png: choose from Receipts -> Gallery.
- deal-planner-demo-flyer.txt: paste into Deals -> Paste flyer OCR text.
- deal-planner-demo-flyer.pdf: choose from Deals -> Choose Flyer PDF.
- deal-planner-demo-flyer.png: choose from Deals -> Choose Flyer Image.
- deal-planner-demo-pantry-label.txt: reference text for pantry label OCR, including hyphenated 16-ounce and 12-count rows, punctuated label cues, and slash/two-digit label dates.
- deal-planner-demo-pantry-label.png: choose from Pantry -> Gallery for multi-item pantry OCR, including hyphenated package-size rows, punctuated label cues, and common non-ISO label dates.
- deal-planner-demo-upc-a.txt: paste or type into Pantry -> Barcode / UPC.
- deal-planner-demo-upc-a.png: display on another screen or print, then scan from Pantry -> Scan.
- SAMPLE_MANIFEST.md: file byte counts and SHA-256 hashes for local and transfer verification.

Expected receipt result: the bundled demo receipt imports grocery line items, ignores total/tender lines, and updates Budget.
Expected flyer result: the bundled demo flyer imports multiple Kroger deals with prices, limits, coupons, and deal scores.
Expected pantry result: the label image imports separate VERIFY pantry rows, including the 16-ounce peanut butter, 12-count eggs, punctuated `net wt:`/`best by:` cues, and slash/two-digit label dates, or shows a visible OCR recovery message if the phone OCR cannot read the generated image.
Expected barcode result: the UPC imports a VERIFY barcode item, using Open Food Facts details when available or fallback barcode details otherwise.
"@

Set-Content -LiteralPath $readmePath -Value $readme -Encoding UTF8
Assert-SampleFolder $sessionDir
$manifestPath = Write-SampleManifest $sessionDir

Write-Host "Created phone test samples:"
Write-Host $sessionDir
Write-Host "Manifest:"
Write-Host $manifestPath
Get-ChildItem -LiteralPath $sessionDir | Select-Object Name, Length
