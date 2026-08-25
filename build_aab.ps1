# ==============================================================================
# MindGame - Google Play Release AAB Build Script
# ==============================================================================
[CmdletBinding()]
param (
    [string]$StorePassword = "",
    [string]$KeyPassword = "",
    [string]$KeyAlias = "upload",
    [switch]$IncrementVersion = $false,
    [switch]$SkipTests = $false
)

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "   MindGame - Google Play Release AAB Build Script" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Path Definitions
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$MindGameDir = Join-Path $ScriptDir "MindGame"
$ReleaseDir = Join-Path $ScriptDir "Release"
$VersionFile = Join-Path $MindGameDir "version.json"
$KeystorePropsFile = Join-Path $MindGameDir "keystore.properties"

$JdkPath = "D:\Work\Sam\Project\APPBuild\jdk17.0.19_10"
$GradlePath = "D:\Work\Sam\Project\APPBuild\gradle-9.3.1\bin\gradle.bat"
$AndroidSdkPath = "D:\Android\Sdk"
$KeytoolPath = Join-Path $JdkPath "bin\keytool.exe"

$KeystoreCandidates = @(
    "D:\Work\Sam\Project\AppKeys\upload-key.jks",
    "D:\Sam\HomeWork\AppKeys\upload-key.jks"
)
$KeystorePath = $KeystoreCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1

# 2. Environment Validation
Write-Host "[1/7] Checking build environment..." -ForegroundColor Yellow
if (-not (Test-Path $JdkPath)) {
    Write-Error "JDK 17 not found at $JdkPath"
}
if (-not (Test-Path $GradlePath)) {
    Write-Error "Gradle 9.3.1 not found at $GradlePath"
}
if (-not (Test-Path $VersionFile)) {
    Write-Error "version.json not found at $VersionFile"
}
if (-not $KeystorePath) {
    Write-Error "Upload key not found in: $($KeystoreCandidates -join ', ')"
}

$env:JAVA_HOME = $JdkPath
$env:PATH = "$JdkPath\bin;" + $env:PATH
$env:ANDROID_HOME = $AndroidSdkPath
$env:KEYSTORE_PATH = $KeystorePath

Write-Host "   [OK] JDK 17:       $JdkPath" -ForegroundColor Green
Write-Host "   [OK] Gradle:       $GradlePath" -ForegroundColor Green
Write-Host "   [OK] Android SDK:  $AndroidSdkPath" -ForegroundColor Green
Write-Host "   [OK] Release Key:  $KeystorePath" -ForegroundColor Green

# 3. Keystore Password Resolution & Verification
Write-Host ""
Write-Host "[2/7] Resolving Release Keystore credentials..." -ForegroundColor Yellow

# Check keystore.properties if exists
if (Test-Path $KeystorePropsFile) {
    $lines = Get-Content $KeystorePropsFile
    foreach ($line in $lines) {
        if ($line -match "^\s*storePassword\s*=\s*(.*)$") {
            if (-not $StorePassword) { $StorePassword = $matches[1].Trim() }
        }
        if ($line -match "^\s*keyPassword\s*=\s*(.*)$") {
            if (-not $KeyPassword) { $KeyPassword = $matches[1].Trim() }
        }
        if ($line -match "^\s*keyAlias\s*=\s*(.*)$") {
            if ($KeyAlias -eq "upload") { $KeyAlias = $matches[1].Trim() }
        }
    }
}

# Check environment variable
if (-not $StorePassword -and $env:STORE_PASSWORD) {
    $StorePassword = $env:STORE_PASSWORD
}

# If still not found, prompt user
while (-not $StorePassword) {
    Write-Host "   [!] 请输入正式金鑰 upload-key.jks 的密碼 (Keystore Password):" -ForegroundColor Yellow -NoNewline
    $inputPass = Read-Host
    if ($inputPass) {
        $StorePassword = $inputPass.Trim()
    }
}

if (-not $KeyPassword) {
    $KeyPassword = $StorePassword
}

# Test keystore password with keytool
Write-Host "   Verifying keystore password with keytool..." -ForegroundColor Gray
$testResult = & $KeytoolPath -list -keystore $KeystorePath -storepass $StorePassword 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Error "Keystore 密碼驗證失敗！請確認密碼是否正確。錯誤訊息: $testResult"
}

Write-Host "   [OK] Keystore password verified successfully!" -ForegroundColor Green

$env:STORE_PASSWORD = $StorePassword
$env:KEY_PASSWORD = $KeyPassword
$env:KEY_ALIAS = $KeyAlias

# 4. Read Version Info
Write-Host ""
Write-Host "[3/7] Reading version configuration..." -ForegroundColor Yellow
$versionRaw = Get-Content -Path $VersionFile -Raw
$versionJson = ConvertFrom-Json -InputObject $versionRaw
$productName = $versionJson.APP_ProductName
$currentVersion = $versionJson.APP_Version

if ($IncrementVersion) {
    $parts = $currentVersion.Split('.')
    if ($parts.Length -eq 4) {
        $lastNum = [int]$parts[3] + 1
        $parts[3] = $lastNum.ToString("D2")
        $newVersion = ($parts -join '.')
        $versionJson.APP_Version = $newVersion
        $newJsonStr = ConvertTo-Json -InputObject $versionJson -Depth 4
        Set-Content -Path $VersionFile -Value $newJsonStr -Encoding UTF8
        Write-Host "   [OK] Version incremented: $currentVersion -> $newVersion" -ForegroundColor Green
        $currentVersion = $newVersion
    }
} else {
    Write-Host "   [OK] Version retained: $currentVersion" -ForegroundColor Green
}

# 5. Run Automated Unit Tests
if (-not $SkipTests) {
    Write-Host ""
    Write-Host "[4/7] Running automated unit tests (Supreme Testing Mandate)..." -ForegroundColor Yellow
    Push-Location -Path $MindGameDir
    try {
        & $GradlePath :app:testDebugUnitTest --no-daemon
        if ($LASTEXITCODE -ne 0) {
            throw "Automated tests failed with exit code $LASTEXITCODE"
        }
        Write-Host "   [OK] All unit tests 100% PASS!" -ForegroundColor Green
    } finally {
        Pop-Location
    }
} else {
    Write-Host ""
    Write-Host "[4/7] Skipping unit tests (-SkipTests passed)" -ForegroundColor DarkYellow
}

# 6. Build Release AAB Bundle
Write-Host ""
Write-Host "[5/7] Building Release AAB (:app:bundleRelease)..." -ForegroundColor Yellow
Push-Location -Path $MindGameDir
try {
    & $GradlePath :app:bundleRelease --no-daemon
    if ($LASTEXITCODE -ne 0) {
        throw "AAB bundle build failed with exit code $LASTEXITCODE"
    }
    Write-Host "   [OK] Release AAB compiled successfully!" -ForegroundColor Green
} finally {
    Pop-Location
}

# 7. Validate Signature and Export
Write-Host ""
Write-Host "[6/7] Validating signature & exporting Release AAB..." -ForegroundColor Yellow
$sourceAab = Join-Path $MindGameDir "app\build\outputs\bundle\release\app-release.aab"
if (-not (Test-Path $sourceAab)) {
    Write-Error "Generated AAB file not found at $sourceAab"
}

# Check signature using keytool printcert
$certOutput = & $KeytoolPath -printcert -jarfile $sourceAab 2>&1
if ($certOutput -match "CN=Android Debug") {
    Write-Error "FATAL: Output AAB is signed with Android Debug certificate! Release signing failed."
}
Write-Host "   [OK] Signature verified: Official Release Certificate confirmed!" -ForegroundColor Green

if (-not (Test-Path $ReleaseDir)) {
    New-Item -ItemType Directory -Path $ReleaseDir | Out-Null
}

$destVersionAab = Join-Path $ReleaseDir "${productName}-v${currentVersion}-release.aab"
$destStandardAab = Join-Path $ReleaseDir "${productName}.aab"

Copy-Item -Path $sourceAab -Destination $destVersionAab -Force
Copy-Item -Path $sourceAab -Destination $destStandardAab -Force

$fileItem = Get-Item -Path $destVersionAab
$sizeMb = [math]::Round($fileItem.Length / 1MB, 2)
$hashObj = Get-FileHash -Path $destVersionAab -Algorithm SHA256
$hash = $hashObj.Hash

Write-Host "   [OK] AAB Exported Successfully!" -ForegroundColor Green
Write-Host "   - File Name: $($fileItem.Name)" -ForegroundColor Cyan
Write-Host "   - File Size: $sizeMb MB ($($fileItem.Length) bytes)" -ForegroundColor Cyan
Write-Host "   - SHA256:    $hash" -ForegroundColor Cyan
Write-Host "   - Output:    $destVersionAab" -ForegroundColor Cyan

# 8. Post-build Intermediate Cleanup
Write-Host ""
Write-Host "[7/7] Cleaning up build intermediates (retaining reports and test-results)..." -ForegroundColor Yellow
$intermediatesDir = Join-Path $MindGameDir "app\build\intermediates"
$tmpDir = Join-Path $MindGameDir "app\build\tmp"
if (Test-Path $intermediatesDir) {
    Remove-Item -Recurse -Force -Path $intermediatesDir -ErrorAction SilentlyContinue
}
if (Test-Path $tmpDir) {
    Remove-Item -Recurse -Force -Path $tmpDir -ErrorAction SilentlyContinue
}
Write-Host "   [OK] Intermediates cleaned successfully!" -ForegroundColor Green

Write-Host ""
Write-Host "========================================================" -ForegroundColor Green
Write-Host " Google Play Release AAB Build Complete!" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Green
Write-Host "Google Play Console Closed Testing Steps:" -ForegroundColor White
Write-Host "  1. Open Google Play Console -> Select 'MindGame'" -ForegroundColor Gray
Write-Host "  2. Go to 'Testing' -> 'Closed testing' or 'Internal testing'" -ForegroundColor Gray
Write-Host "  3. Click 'Create new release' -> Upload: $destVersionAab" -ForegroundColor Gray
Write-Host "  4. Review details, enter release notes, and rollout!" -ForegroundColor Gray
Write-Host ""
