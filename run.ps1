
$ErrorActionPreference = "Stop"

mvn javafx:run

if ($LASTEXITCODE -ne 0) {
    Write-Host "No se pudo ejecutar Platea Desktop."
    exit $LASTEXITCODE
}
  