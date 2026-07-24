$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent $PSScriptRoot
$stagingDir = Join-Path $repoRoot "coffee-mall-backend-src"
$zipPath = Join-Path $repoRoot "coffee-mall-backend-src.zip"

$includeItems = @(
    "pom.xml",
    "src",
    "sql",
    "README.md",
    "ry.sh",
    "ry.bat",
    "上线前清理与部署说明.md"
)

if (Test-Path $stagingDir) {
    Remove-Item -Path $stagingDir -Recurse -Force
}

if (Test-Path $zipPath) {
    Remove-Item -Path $zipPath -Force
}

New-Item -ItemType Directory -Path $stagingDir | Out-Null

foreach ($item in $includeItems) {
    $sourcePath = Join-Path $repoRoot $item
    if (-not (Test-Path $sourcePath)) {
        continue
    }

    $destinationPath = Join-Path $stagingDir $item
    $parentPath = Split-Path -Parent $destinationPath
    if (-not (Test-Path $parentPath)) {
        New-Item -ItemType Directory -Path $parentPath -Force | Out-Null
    }

    Copy-Item -Path $sourcePath -Destination $destinationPath -Recurse -Force
}

Compress-Archive -Path (Join-Path $stagingDir "*") -DestinationPath $zipPath -Force

Write-Output "Created archive: $zipPath"
Write-Output "Staging directory: $stagingDir"
