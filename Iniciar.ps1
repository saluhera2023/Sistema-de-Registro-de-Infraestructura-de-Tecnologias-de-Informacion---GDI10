param([switch]$PrepararSinAbrir)
$ErrorActionPreference = 'Stop'
if (-not [Environment]::Is64BitOperatingSystem) { throw 'Este aplicativo requiere Windows de 64 bits.' }
$raiz = $PSScriptRoot
$datos = Join-Path $raiz 'datos'
$cluster = Join-Path $datos 'postgresql'
$archivoConfiguracion = Join-Path $datos 'configuracion.json'
$registro = Join-Path $datos 'inicio.log'
$utf8 = New-Object Text.UTF8Encoding($false)
$null = New-Item -ItemType Directory -Force -Path $datos
$config = $null
$pg = ''
$detener = $false
$adquirido = $false
$hash = [Security.Cryptography.SHA256]::Create()
try { $id = ([BitConverter]::ToString($hash.ComputeHash($utf8.GetBytes($datos.ToUpperInvariant())))).Replace('-', '') }
finally { $hash.Dispose() }
$mutex = New-Object Threading.Mutex($false, "Local\RegistroInfraestructuraTI_$id")

# Conserva los argumentos completos, incluso si la ruta contiene espacios.
function Citar-Argumento([string]$Texto) {
    if ($Texto.Length -gt 0 -and $Texto -notmatch '[\s"]') { return $Texto }
    $valor = [regex]::Replace($Texto, '(\\*)"', '$1$1\"')
    $valor = [regex]::Replace($valor, '(\\+)$', '$1$1')
    return '"' + $valor + '"'
}

function Ejecutar-Nativo([string]$Archivo, [string[]]$Argumentos, [int]$Limite = 60000, [switch]$PermitirError) {
    $info = New-Object Diagnostics.ProcessStartInfo
    $info.FileName = $Archivo
    $info.Arguments = (($Argumentos | ForEach-Object { Citar-Argumento $_ }) -join ' ')
    $info.WorkingDirectory = $raiz
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $info.StandardOutputEncoding = $utf8
    $info.StandardErrorEncoding = $utf8
    foreach ($nombre in @($info.EnvironmentVariables.Keys)) {
        if ($nombre -match '^PG') { $info.EnvironmentVariables.Remove($nombre) }
    }
    if ($pg) { $info.EnvironmentVariables['PATH'] = $pg + [IO.Path]::PathSeparator + $env:PATH }
    $info.EnvironmentVariables['PGCLIENTENCODING'] = 'UTF8'
    $info.EnvironmentVariables['PGCONNECT_TIMEOUT'] = '5'
    $info.EnvironmentVariables['PGSSLMODE'] = 'disable'
    $info.EnvironmentVariables['PGGSSENCMODE'] = 'disable'
    if ($config) { $info.EnvironmentVariables['PGPASSWORD'] = $config.Clave }
    $proceso = New-Object Diagnostics.Process
    $proceso.StartInfo = $info
    try {
        $null = $proceso.Start()
        $salida = $proceso.StandardOutput.ReadToEndAsync()
        $errores = $proceso.StandardError.ReadToEndAsync()
        if (-not $proceso.WaitForExit($Limite)) { $proceso.Kill(); throw 'La operación tardó demasiado. Revisa datos/inicio.log.' }
        # pg_ctl puede dejar tuberías abiertas en el servidor que acaba de iniciar.
        $texto = if ($salida.Wait(1500)) { $salida.GetAwaiter().GetResult() } else { '' }
        $errorTexto = if ($errores.Wait(1500)) { $errores.GetAwaiter().GetResult() } else { '' }
        [IO.File]::AppendAllText($registro, $texto + $errorTexto, $utf8)
        $resultado = [pscustomobject]@{ Codigo = $proceso.ExitCode; Texto = $texto.Trim() }
        if ($resultado.Codigo -ne 0 -and -not $PermitirError) { throw "Falló $([IO.Path]::GetFileName($Archivo)). Revisa datos/inicio.log." }
        return $resultado
    } finally { $proceso.Dispose() }
}

function Herramienta-Java([string]$Nombre) {
    if ($env:JAVA_HOME) {
        $ruta = Join-Path $env:JAVA_HOME "bin/$Nombre.exe"
        if (Test-Path -LiteralPath $ruta -PathType Leaf) { return $ruta }
    }
    $comando = Get-Command $Nombre -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($comando) { return [string]$comando.Source }
    return $null
}

function Buscar-Java {
    $incluido = Join-Path $raiz 'runtime/java/bin/java.exe'
    if (Test-Path -LiteralPath $incluido -PathType Leaf) { return $incluido }
    $javac = Herramienta-Java 'javac'
    if ($javac) {
        $pareja = Join-Path (Split-Path -Parent $javac) 'java.exe'
        if (Test-Path -LiteralPath $pareja -PathType Leaf) { return $pareja }
    }
    $java = Herramienta-Java 'java'
    if ($java) { return $java }
    throw 'Falta Java. Usa el paquete completo para Windows o instala un JDK 17 o posterior.'
}

function Buscar-PostgreSQL {
    $candidatos = @((Join-Path $raiz 'runtime/postgresql/bin'))
    $comando = Get-Command 'initdb' -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($comando) { $candidatos += Split-Path -Parent $comando.Source }
    if ($env:ProgramFiles) {
        $instalados = Join-Path $env:ProgramFiles 'PostgreSQL'
        if (Test-Path -LiteralPath $instalados) {
            foreach ($carpeta in @(Get-ChildItem -LiteralPath $instalados -Directory | Sort-Object Name -Descending)) {
                $candidatos += Join-Path $carpeta.FullName 'bin'
            }
        }
    }
    $versionDatos = if (Test-Path -LiteralPath (Join-Path $cluster 'PG_VERSION')) { (Get-Content -LiteralPath (Join-Path $cluster 'PG_VERSION') -Raw).Trim() } else { '' }
    foreach ($candidato in @($candidatos | Select-Object -Unique)) {
        $completo = $true
        foreach ($nombre in @('postgres.exe','initdb.exe','pg_ctl.exe','psql.exe','pg_controldata.exe')) {
            if (-not (Test-Path -LiteralPath (Join-Path $candidato $nombre) -PathType Leaf)) { $completo = $false; break }
        }
        if (-not $completo) { continue }
        $version = Ejecutar-Nativo (Join-Path $candidato 'postgres.exe') @('--version')
        $mayor = [regex]::Match($version.Texto, '(\d+)(?:\.\d+)?').Groups[1].Value
        if ([int]$mayor -ge 17 -and (-not $versionDatos -or $mayor -eq $versionDatos)) { return $candidato }
    }
    throw 'No se encontró un PostgreSQL compatible. Usa el paquete completo de Windows o instala PostgreSQL 17 o posterior.'
}

function Consultar([string]$Base, [string]$SQL) {
    return (Ejecutar-Nativo (Join-Path $pg 'psql.exe') @('-X','-w','-h','127.0.0.1','-p',[string]$config.Puerto,'-U','inventario','-d',$Base,'-v','ON_ERROR_STOP=1','-At','-c',$SQL)).Texto
}

function Puerto-Libre {
    $escucha = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback, 0)
    try { $escucha.Start(); return $escucha.LocalEndpoint.Port }
    finally { $escucha.Stop() }
}

try {
    try { $adquirido = $mutex.WaitOne(0) }
    catch [Threading.AbandonedMutexException] { $adquirido = $true }
    if (-not $adquirido) { Write-Output 'El sistema ya está abierto desde esta carpeta.'; exit 0 }
    $java = Buscar-Java
    $ejecutable = Join-Path $raiz 'bin/RegistroInfraestructuraTI.jar'
    $recompilar = -not (Test-Path -LiteralPath $ejecutable -PathType Leaf)
    if (-not $recompilar) {
        $fechaJar = (Get-Item -LiteralPath $ejecutable).LastWriteTimeUtc
        $recompilar = @((Get-ChildItem -LiteralPath (Join-Path $raiz 'src') -Recurse -Filter '*.java') | Where-Object { $_.LastWriteTimeUtc -gt $fechaJar }).Count -gt 0
    }
    if ($recompilar) {
        Write-Output 'Compilando el código fuente.'
        $hostPowerShell = Join-Path $PSHOME 'powershell.exe'
        if (-not (Test-Path -LiteralPath $hostPowerShell)) { $hostPowerShell = Join-Path $PSHOME 'pwsh' }
        $null = Ejecutar-Nativo $hostPowerShell @('-NoLogo','-NoProfile','-ExecutionPolicy','Bypass','-File',(Join-Path $raiz 'Compilar.ps1')) -Limite 120000
    }
    if (-not (Test-Path -LiteralPath $ejecutable -PathType Leaf)) { throw 'No se encontró el aplicativo compilado.' }
    $pg = Buscar-PostgreSQL

    # Las credenciales pertenecen a esta copia del aplicativo y se generan una sola vez.
    if (Test-Path -LiteralPath $archivoConfiguracion) {
        $config = Get-Content -LiteralPath $archivoConfiguracion -Raw -Encoding UTF8 | ConvertFrom-Json
    } else {
        if (Test-Path -LiteralPath (Join-Path $cluster 'PG_VERSION')) { throw 'Falta datos/configuracion.json. Recupera ese archivo antes de abrir la base existente.' }
        $bytes = New-Object byte[] 32
        $azar = [Security.Cryptography.RandomNumberGenerator]::Create()
        try { $azar.GetBytes($bytes) } finally { $azar.Dispose() }
        $config = [pscustomobject]@{ Clave = [Convert]::ToBase64String($bytes); Puerto = 0 }
        [IO.File]::WriteAllText($archivoConfiguracion, ($config | ConvertTo-Json), $utf8)
    }
    if (-not (Test-Path -LiteralPath (Join-Path $cluster 'PG_VERSION'))) {
        Write-Output 'Preparando la base del sistema por primera vez.'
        $temporal = Join-Path $datos ('inicializando_' + [guid]::NewGuid().ToString('N'))
        $claveTemporal = Join-Path $datos ('clave_' + [guid]::NewGuid().ToString('N') + '.tmp')
        [IO.File]::WriteAllText($claveTemporal, $config.Clave, $utf8)
        try {
            $null = Ejecutar-Nativo (Join-Path $pg 'initdb.exe') @('-D',$temporal,'-U','inventario','-A','scram-sha-256','--pwfile',$claveTemporal,'--no-locale','--encoding=UTF8') -Limite 120000
            Move-Item -LiteralPath $temporal -Destination $cluster
        } finally {
            Remove-Item -LiteralPath $claveTemporal -Force -ErrorAction SilentlyContinue
            if (Test-Path -LiteralPath $temporal) { Remove-Item -LiteralPath $temporal -Recurse -Force }
        }
    }
    $estado = Ejecutar-Nativo (Join-Path $pg 'pg_ctl.exe') @('-D',$cluster,'status') -PermitirError
    if ($estado.Codigo -eq 0) {
        $config.Puerto = [int](Get-Content -LiteralPath (Join-Path $cluster 'postmaster.pid'))[3]
        $detener = $true
    } else {
        for ($intento = 0; $intento -lt 3; $intento++) {
            $config.Puerto = Puerto-Libre
            $opciones = "-h 127.0.0.1 -p $($config.Puerto) -c max_connections=15 -c shared_buffers=16MB"
            $inicio = Ejecutar-Nativo (Join-Path $pg 'pg_ctl.exe') @('-D',$cluster,'-l',(Join-Path $datos 'postgresql.log'),'-o',$opciones,'-w','-t','25','start') -Limite 35000 -PermitirError
            if ($inicio.Codigo -eq 0) { $detener = $true; break }
        }
        if (-not $detener) { throw 'No se pudo iniciar la base. Revisa datos/postgresql.log.' }
    }
    [IO.File]::WriteAllText($archivoConfiguracion, ($config | ConvertTo-Json), $utf8)
    $control = Ejecutar-Nativo (Join-Path $pg 'pg_controldata.exe') @($cluster)
    $idLocal = [regex]::Match($control.Texto, '(?m):\s*(\d{15,})\s*$').Groups[1].Value
    $idServidor = Consultar 'postgres' 'SELECT system_identifier FROM pg_control_system()'
    if (-not $idLocal -or $idLocal -ne $idServidor) { $detener = $false; throw 'El puerto pertenece a otra carpeta de datos.' }

    # La aplicación prepara las tablas únicamente si la base está vacía.
    $configJava = @('servidor=127.0.0.1',"puerto=$($config.Puerto)",'base=inventario_ti','usuario=inventario',"clave=$($config.Clave)") -join [Environment]::NewLine
    [IO.File]::WriteAllText((Join-Path $datos 'configuracion.properties'), $configJava, $utf8)
    $classpath = $ejecutable + [IO.Path]::PathSeparator + (Join-Path $raiz 'lib/postgresql-42.7.11.jar')
    $opcionConfig = "-Dinventario.config.dir=$datos"
    $null = Ejecutar-Nativo $java @($opcionConfig,'-cp',$classpath,'inventario.App','--preparar') -Limite 120000
    if (-not $PrepararSinAbrir) {
        Write-Output 'Abriendo el sistema.'
        $null = Ejecutar-Nativo $java @($opcionConfig,'-cp',$classpath,'inventario.App') -Limite 2147483647
    }
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
} finally {
    if ($detener) {
        try { $null = Ejecutar-Nativo (Join-Path $pg 'pg_ctl.exe') @('-D',$cluster,'-m','fast','-w','-t','20','stop') -Limite 25000 -PermitirError }
        catch { Write-Output 'Revisa datos/postgresql.log si necesitas comprobar el cierre.' }
    }
    if ($adquirido) { $mutex.ReleaseMutex() }
    $mutex.Dispose()
}
