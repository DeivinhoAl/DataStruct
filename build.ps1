$ErrorActionPreference = 'Stop'
$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$classesDir = Join-Path $projectDir 'out\release'
$distDir = Join-Path $projectDir 'dist'
if (Test-Path -LiteralPath $classesDir) {
    $resolvedClasses = (Resolve-Path -LiteralPath $classesDir).Path
    $expectedClasses = [System.IO.Path]::GetFullPath((Join-Path $projectDir 'out\release'))
    if ($resolvedClasses -ne $expectedClasses -or (Get-Item -LiteralPath $classesDir).Attributes.HasFlag([System.IO.FileAttributes]::ReparsePoint)) {
        throw 'Diretório de compilação inesperado. Limpeza cancelada.'
    }
    Remove-Item -LiteralPath $resolvedClasses -Recurse -Force
}
New-Item -ItemType Directory -Path $classesDir, $distDir -Force | Out-Null
$sources = @(Get-ChildItem -LiteralPath (Join-Path $projectDir 'src') -Recurse -Filter '*.java' -File | ForEach-Object FullName)
& javac --release 17 -encoding UTF-8 -d $classesDir $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& (Join-Path $projectDir 'resources.ps1') -OutputDir $classesDir
$jarFile = Join-Path $distDir 'DataStruct.jar'
& jar --create --file $jarFile --main-class datastruct.DataStructApp -C $classesDir .
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Output "JAR criado: $jarFile"
