$ErrorActionPreference = 'Stop'
$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$outDir = Join-Path $projectDir 'out'
New-Item -ItemType Directory -Path $outDir -Force | Out-Null
$sources = @(Get-ChildItem -LiteralPath (Join-Path $projectDir 'src') -Recurse -Filter '*.java' -File | ForEach-Object FullName)
& javac --release 17 -encoding UTF-8 -d $outDir $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& (Join-Path $projectDir 'resources.ps1') -OutputDir $outDir
& java -cp $outDir datastruct.DataStructApp
exit $LASTEXITCODE
