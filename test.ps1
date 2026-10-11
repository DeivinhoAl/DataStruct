param([switch]$Preview)
$ErrorActionPreference = 'Stop'
$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$outDir = Join-Path $projectDir 'out'
New-Item -ItemType Directory -Path $outDir -Force | Out-Null
$sources = @(
    Get-ChildItem -LiteralPath (Join-Path $projectDir 'src') -Recurse -Filter '*.java' -File
    Get-ChildItem -LiteralPath (Join-Path $projectDir 'test') -Recurse -Filter '*.java' -File
) | ForEach-Object FullName
& javac --release 17 -encoding UTF-8 -d $outDir $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& (Join-Path $projectDir 'resources.ps1') -OutputDir $outDir
& java -ea -cp $outDir datastruct.SimulatorTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $outDir datastruct.CanvasSmokeTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$previewArgs = @()
if ($Preview) { $previewArgs = @((Join-Path $outDir 'design-review')) }
& java -cp $outDir datastruct.WorkbenchTest @previewArgs
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $outDir datastruct.StudyTest
exit $LASTEXITCODE
