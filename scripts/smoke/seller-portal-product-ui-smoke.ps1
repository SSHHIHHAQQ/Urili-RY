param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [string]$FrontendUrl = "http://127.0.0.1:8001",
    [string]$AdminUsername = "admin",
    [string]$AdminPassword = $env:URILI_ADMIN_PASSWORD,
    [long]$SellerId = 5,
    [string]$Reason = "codex seller portal product ui smoke",
    [string]$BrowserChannel = "chrome",
    [string]$ExecutablePath = "",
    [switch]$Headed,
    [int]$TimeoutMs = 30000,
    [string]$ScreenshotPath = "",
    [string]$PlaywrightRequireFrom = "",
    [string]$RuntimeDir = "$env:TEMP\urili-playwright-smoke-runtime"
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($AdminPassword)) {
    throw "AdminPassword is required. Pass -AdminPassword or set URILI_ADMIN_PASSWORD."
}

function Resolve-PlaywrightRequireFrom {
    param(
        [string]$RequestedPath,
        [string]$InstallRuntimeDir
    )

    if (-not [string]::IsNullOrWhiteSpace($RequestedPath)) {
        if (-not (Test-Path $RequestedPath)) {
            throw "PlaywrightRequireFrom does not exist: $RequestedPath"
        }
        return (Resolve-Path $RequestedPath).Path
    }

    $npmCommand = (Get-Command npm.cmd -ErrorAction SilentlyContinue)
    if ($null -eq $npmCommand) {
        $npmCommand = Get-Command npm -ErrorAction Stop
    }
    $npmRootOutput = & $npmCommand.Source root -g
    $npmRoot = ($npmRootOutput | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -First 1).Trim()
    if ([string]::IsNullOrWhiteSpace($npmRoot)) {
        $npmRoot = Join-Path $env:APPDATA "npm\node_modules"
    }

    $globalCliPackage = Join-Path $npmRoot "@playwright\cli\package.json"
    $globalCliPlaywright = Join-Path $npmRoot "@playwright\cli\node_modules\playwright\package.json"
    if ((Test-Path $globalCliPackage) -and (Test-Path $globalCliPlaywright)) {
        return (Resolve-Path $globalCliPackage).Path
    }

    $runtimePackage = Join-Path $InstallRuntimeDir "package.json"
    $runtimePlaywright = Join-Path $InstallRuntimeDir "node_modules\playwright\package.json"
    if (-not (Test-Path $runtimePlaywright)) {
        New-Item -ItemType Directory -Force -Path $InstallRuntimeDir | Out-Null
        Push-Location $InstallRuntimeDir
        try {
            if (-not (Test-Path $runtimePackage)) {
                & $npmCommand.Source init -y | Out-Null
            }
            $previousSkipDownload = $env:PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD
            $env:PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD = "1"
            try {
                & $npmCommand.Source install playwright --no-save
            }
            finally {
                $env:PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD = $previousSkipDownload
            }
        }
        finally {
            Pop-Location
        }
    }

    if (-not (Test-Path $runtimePlaywright)) {
        throw "Unable to prepare Playwright runtime under $InstallRuntimeDir"
    }
    return (Resolve-Path $runtimePackage).Path
}

$scriptPath = Join-Path $PSScriptRoot "seller-portal-product-ui-smoke.mjs"
$resolvedPlaywrightRequireFrom = Resolve-PlaywrightRequireFrom `
    -RequestedPath $PlaywrightRequireFrom `
    -InstallRuntimeDir $RuntimeDir

$nodeArgs = @(
    $scriptPath,
    "--base-url", $BaseUrl,
    "--frontend-url", $FrontendUrl,
    "--admin-username", $AdminUsername,
    "--admin-password", $AdminPassword,
    "--seller-id", "$SellerId",
    "--reason", $Reason,
    "--timeout-ms", "$TimeoutMs",
    "--playwright-require-from", $resolvedPlaywrightRequireFrom
)

if (-not [string]::IsNullOrWhiteSpace($ExecutablePath)) {
    $nodeArgs += @("--executable-path", $ExecutablePath)
}
elseif (-not [string]::IsNullOrWhiteSpace($BrowserChannel)) {
    $nodeArgs += @("--browser-channel", $BrowserChannel)
}

if ($Headed) {
    $nodeArgs += "--headed"
}

if (-not [string]::IsNullOrWhiteSpace($ScreenshotPath)) {
    $nodeArgs += @("--screenshot-path", $ScreenshotPath)
}

& node @nodeArgs
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
