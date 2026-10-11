param([Parameter(Mandatory=$true)][string]$OutputDir)
$ErrorActionPreference = 'Stop'
$resourceProject = Split-Path -Parent $MyInvocation.MyCommand.Path
$assetTarget = Join-Path $OutputDir 'assets'
$sourceTarget = Join-Path $OutputDir 'source'
New-Item -ItemType Directory -Path $assetTarget, $sourceTarget -Force | Out-Null
Copy-Item -LiteralPath (Join-Path $resourceProject 'assets\cover.png') -Destination (Join-Path $assetTarget 'cover.png') -Force
Copy-Item -LiteralPath (Join-Path $resourceProject 'src\datastruct\core\Simulator.java') -Destination (Join-Path $sourceTarget 'Simulator.java') -Force
