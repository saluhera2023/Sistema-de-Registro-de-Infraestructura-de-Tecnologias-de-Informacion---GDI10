$ErrorActionPreference = 'Stop'

function Buscar-Herramienta([string]$Nombre) {
    if ($env:JAVA_HOME) {
        $archivo = Join-Path $env:JAVA_HOME "bin/$Nombre.exe"
        if (Test-Path -LiteralPath $archivo -PathType Leaf) { return $archivo }
    }
    $comando = Get-Command $Nombre -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($comando) { return [string]$comando.Source }
    throw "Para compilar el código se requiere un JDK 17 o posterior: falta $Nombre."
}

try {
    $javac = Buscar-Herramienta 'javac'
    $jar = Join-Path (Split-Path -Parent $javac) 'jar.exe'
    if (-not (Test-Path -LiteralPath $jar -PathType Leaf)) { $jar = Buscar-Herramienta 'jar' }
    $clases = Join-Path $PSScriptRoot 'build/classes'
    $bin = Join-Path $PSScriptRoot 'bin'
    $null = New-Item -ItemType Directory -Force -Path $clases, $bin
    $fuentes = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src') -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName)
    $controlador = Join-Path $PSScriptRoot 'lib/postgresql-42.7.11.jar'
    & $javac --release 17 -encoding UTF-8 -cp $controlador -d $clases @fuentes
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo compilar. Revisa los mensajes anteriores.' }

    # Crea un solo ejecutable a partir de todos los módulos.
    $temporal = Join-Path $bin 'RegistroInfraestructuraTI.temporal.jar'
    & $jar --create --file $temporal --main-class inventario.App -C $clases .
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo generar el archivo JAR.' }
    Move-Item -LiteralPath $temporal -Destination (Join-Path $bin 'RegistroInfraestructuraTI.jar') -Force
    Write-Output 'Código compilado correctamente.'
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}
