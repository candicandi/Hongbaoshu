Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Die {
    param([string]$Message)
    Write-Error $Message
    exit 1
}

function Ensure-Java {
    $candidateJavaHomes = @(
        "$env:JAVA_HOME",
        "D:\Android Studio\jbr",
        "C:\Program Files\Android\Android Studio\jbr",
        "C:\Program Files\Android\Android Studio Preview\jbr"
    ) | Where-Object { $_ -and $_.Trim() -ne "" }

    $resolvedJavaExe = $null

    try {
        & java -version *> $null
        $javaCmd = Get-Command java -ErrorAction SilentlyContinue
        if ($javaCmd) {
            $resolvedJavaExe = $javaCmd.Source
        }
    } catch {
        # Continue to fallback discovery.
    }

    if (-not $resolvedJavaExe) {
        foreach ($candidate in $candidateJavaHomes) {
            $javaExe = Join-Path $candidate "bin\java.exe"
            if (Test-Path $javaExe) {
                $env:JAVA_HOME = $candidate
                $env:Path = "$(Join-Path $candidate 'bin');$env:Path"
                $resolvedJavaExe = $javaExe
                break
            }
        }
    }

    if (-not $resolvedJavaExe -or -not (Test-Path $resolvedJavaExe)) {
        Die "Unable to locate a Java Runtime. Install JDK 17+ (or Android Studio) and ensure 'java' is available."
    }

    foreach ($candidate in $candidateJavaHomes) {
        $javaExe = Join-Path $candidate "bin\java.exe"
        if ($resolvedJavaExe -eq $javaExe) {
            $env:JAVA_HOME = $candidate
            break
        }
    }

    # Final sync to ensure child processes resolve java from the selected JAVA_HOME.
    if ($env:JAVA_HOME) {
        $env:Path = "$(Join-Path $env:JAVA_HOME 'bin');$env:Path"
    }
}

function Resolve-Adb {
    $adbCmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($adbCmd) {
        return $adbCmd.Source
    }

    $localProps = Join-Path $PSScriptRoot "local.properties"
    if (Test-Path $localProps) {
            $sdkLine = Get-Content -Encoding UTF8 $localProps | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
            if ($sdkLine) {
                $sdkDirRaw = $sdkLine -replace '^sdk\.dir=', ''
                $sdkDir = $sdkDirRaw -replace '\\\\', '\' -replace '\\:', ':'
                $adbFromSdk = Join-Path $sdkDir "platform-tools\adb.exe"
                if (Test-Path $adbFromSdk) {
                    return $adbFromSdk
                }
            }
    }

    Die "adb not found. Install Android Platform Tools or set sdk.dir in local.properties."
}

$gradleWrapper = Join-Path $PSScriptRoot "gradlew.bat"
if (-not (Test-Path $gradleWrapper)) {
    Die "gradlew.bat not found."
}

Ensure-Java
$adb = Resolve-Adb

Write-Host "Starting automated build..."
& $gradleWrapper assembleDebug
if ($LASTEXITCODE -ne 0) {
    Die "Build failed. Please check logs above."
}
Write-Host "Build successful."

$apkPath = Join-Path $PSScriptRoot "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apkPath)) {
    Die "APK not found at $apkPath"
}

$deviceLines = & $adb devices | Select-Object -Skip 1
$devices = @()
foreach ($line in $deviceLines) {
    if ($line -match '^\s*([^\s]+)\s+device\s*$') {
        $devices += $matches[1]
    }
}

if ($devices.Count -eq 0) {
    Write-Warning "No devices connected."
    exit 0
}

foreach ($device in $devices) {
    Write-Host "----------------------------------------"
    Write-Host "Processing device: $device"

    Write-Host "  Uninstalling previous version..."
    & $adb -s $device uninstall com.xuyutech.hongbaoshu *> $null

    Write-Host "  Installing new version..."
    & $adb -s $device install -r $apkPath
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  Install successful."
        Write-Host "  Launching app..."
        & $adb -s $device shell am start -n com.xuyutech.hongbaoshu/com.xuyutech.hongbaoshu.MainActivity *> $null
    } else {
        Write-Warning "Install failed on $device"
    }
}

Write-Host "----------------------------------------"
Write-Host "All devices processed."
