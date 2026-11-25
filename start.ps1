# Lance tout Blasa : Redis (Docker) + donnees de demo + API et page web.
# Usage (dans le dossier blasa) :   .\start.ps1            demarre
#                                   .\start.ps1 -Reset     remet les donnees de demo a zero puis demarre
param([switch]$Reset)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

# --- Java 21 : JAVA_HOME ou installation Temurin standard
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $jdk = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -ErrorAction SilentlyContinue |
           Where-Object Name -like "jdk-21*" | Select-Object -First 1
    if (-not $jdk) { throw "JDK 21 introuvable. Installe-le : winget install EclipseAdoptium.Temurin.21.JDK" }
    $env:JAVA_HOME = $jdk.FullName
}
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# --- Maven : dans le PATH, ou dans %USERPROFILE%\tools
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    $mvn = Get-ChildItem "$env:USERPROFILE\tools" -Directory -Filter "apache-maven-*" -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $mvn) { throw "Maven introuvable (attendu dans $env:USERPROFILE\tools\apache-maven-*)." }
    $env:Path = "$($mvn.FullName)\bin;$env:Path"
}

# --- Redis dans Docker
Write-Host "1/3 Redis (Docker)..." -ForegroundColor Cyan
docker compose up -d
if ($LASTEXITCODE -ne 0) { throw "docker compose a echoue : Docker Desktop est-il lance ? Un autre conteneur nomme blasa-redis existe-t-il deja (docker rm -f blasa-redis) ?" }
for ($i = 0; $i -lt 20; $i++) {
    if ((docker exec blasa-redis redis-cli ping) -eq "PONG") { break }
    Start-Sleep 1
}

# --- Donnees de demo : generees avec des dates de depart FRAICHES (demain et apres-demain)
function New-Seed {
    $t = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds() + 20 * 3600
    $t = $t - ($t % 3600)                                   # heure pile, dans ~20 h
    $offsets = @(0, 1, 3, 26, 30, 2, 4, 28)                 # decalage (en heures) de chaque trajet
    $c = [IO.File]::ReadAllText("$PSScriptRoot\redis\01_seed.template.redis")
    for ($i = 1; $i -le $offsets.Count; $i++) {
        $c = $c.Replace("{{DEP$i}}", [string]($t + $offsets[$i - 1] * 3600))
    }
    [IO.File]::WriteAllText("$PSScriptRoot\redis\01_seed.redis", $c, (New-Object Text.UTF8Encoding($false)))
}

# On recharge si : base vide, option -Reset, ou si le trajet 1 a deja expire (depart passe)
$size = [int](docker exec blasa-redis redis-cli dbsize)
$ride1 = [int](docker exec blasa-redis redis-cli exists ride:1)
if ($Reset -or $size -eq 0 -or $ride1 -eq 0) {
    Write-Host "2/3 Chargement des donnees de demo (dates fraiches)..." -ForegroundColor Cyan
    New-Seed
    cmd /c "docker exec -i blasa-redis redis-cli < redis\01_seed.redis" | Out-Null
} else {
    Write-Host "2/3 Donnees deja presentes ($size cles). Utilise -Reset pour repartir de zero." -ForegroundColor Cyan
}

# --- API + page web
Write-Host "3/3 API Blasa sur http://localhost:8080  (Ctrl+C pour arreter)" -ForegroundColor Cyan
# ouvre le navigateur quand le serveur est pret
Start-Job { for ($i = 0; $i -lt 60; $i++) { try { Invoke-WebRequest "http://localhost:8080/" -UseBasicParsing | Out-Null; Start-Process "http://localhost:8080/"; break } catch { Start-Sleep 1 } } } | Out-Null
mvn -q compile exec:java
