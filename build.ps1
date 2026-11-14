# ================================================================
# DGoshi — Script de build
# Uso:
#   .\build.ps1 jar     → compila fat JAR (precisa só de Java/GraalVM)
#   .\build.ps1 native  → compila executável nativo (precisa de GraalVM + MSVC no Windows / GCC no Linux)
#   .\build.ps1 clean   → limpa o target
# ================================================================

param(
    [Parameter(Position=0)]
    [ValidateSet("jar", "native", "clean", "test")]
    [string]$Mode = "jar"
)

$ErrorActionPreference = "Stop"

# Detecta GraalVM ou fallback para IntelliJ bundled Maven
$graalvm = "C:\Users\talita\.jdks\graalvm-jdk-25"
if (Test-Path "$graalvm\bin\java.exe") {
    $env:JAVA_HOME = $graalvm
    $env:PATH = "$graalvm\bin;$env:PATH"
    Write-Host "Usando GraalVM: $graalvm" -ForegroundColor Green
} else {
    $ideaJbr = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.3\jbr"
    if (Test-Path "$ideaJbr\bin\java.exe") {
        $env:JAVA_HOME = $ideaJbr
        Write-Host "GraalVM nao encontrado, usando IntelliJ JBR: $ideaJbr" -ForegroundColor Yellow
    } else {
        Write-Host "ERRO: Nem GraalVM nem JBR encontrados. Configure JAVA_HOME manualmente." -ForegroundColor Red
        exit 1
    }
}

# Encontra Maven
$mvn = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd"
if (-not (Test-Path $mvn)) {
    $mvn = "mvn"
}

switch ($Mode) {
    "jar" {
        Write-Host "`n=== Build JAR ===`n" -ForegroundColor Cyan
        & $mvn clean package -DskipTests
        if ($LASTEXITCODE -eq 0) {
            Write-Host "`n✅ JAR gerado: target\DGoshi-0.0.1.jar" -ForegroundColor Green
            Write-Host "   Rode com: java -jar target\DGoshi-0.0.1.jar`n" -ForegroundColor Gray
        }
    }
    "native" {
        Write-Host "`n=== Build Nativo ===`n" -ForegroundColor Cyan
        if (-not (Test-Path "$graalvm\bin\native-image.cmd")) {
            Write-Host "ERRO: native-image nao encontrado em $graalvm\bin" -ForegroundColor Red
            Write-Host "   Instale o GraalVM e rode: gu install native-image" -ForegroundColor Yellow
            exit 1
        }
        & $mvn -Pnative clean package -DskipTests
        if ($LASTEXITCODE -eq 0) {
            $exe = "target\dgoshi.exe"
            if (Test-Path $exe) {
                $size = [math]::Round((Get-Item $exe).Length / 1MB, 1)
                Write-Host "`n✅ Executavel nativo gerado: $exe ($size MB)" -ForegroundColor Green
                Write-Host "   Rode com: .\target\dgoshi.exe`n" -ForegroundColor Gray
            } else {
                Write-Host "`n✅ Build concluido. Verifique target\ para o binario." -ForegroundColor Green
            }
        }
    }
    "test" {
        Write-Host "`n=== Testes ===`n" -ForegroundColor Cyan
        & $mvn test
    }
    "clean" {
        Write-Host "`n=== Clean ===`n" -ForegroundColor Cyan
        & $mvn clean
        Write-Host "`n✅ Target limpo.`n" -ForegroundColor Green
    }
}
