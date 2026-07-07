# =====================================================================
# build.ps1 — 단일모듈 Spring Boot 프로젝트 Maven 빌드 래퍼
#
# 머신 전용값(JDK·mvn·settings·SSL 우회)은 build.env.ps1 에서 읽는다.
# (build.env.ps1 은 gitignore. 없으면 build.env.example.ps1 복사해 생성)
#
# 사용:
#   powershell -NoProfile -ExecutionPolicy Bypass -File .claude/scripts/build.ps1 [goals...]
#   예) build.ps1                 -> clean package (기본)
#       build.ps1 clean compile
#       build.ps1 -DskipTests package
# =====================================================================
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $Goals
)

$ErrorActionPreference = 'Stop'
$scriptDir  = $PSScriptRoot
$projectDir = Resolve-Path (Join-Path $scriptDir '..\..\project')

# 1) 머신 전용 설정 로드(있으면). 없으면 환경변수/기본값 사용.
$envFile = Join-Path $scriptDir 'build.env.ps1'
if (Test-Path $envFile) {
    Write-Host "[build] env: $envFile"
    . $envFile
} else {
    Write-Host "[build] (build.env.ps1 없음 — 환경변수/기본값 사용. 예시는 build.env.example.ps1)"
}

# 2) JAVA_HOME
if ($BUILD_JAVA_HOME) { $env:JAVA_HOME = $BUILD_JAVA_HOME }
if (-not $env:JAVA_HOME) {
    Write-Error "JAVA_HOME 미설정 — build.env.ps1 의 `$BUILD_JAVA_HOME 또는 환경변수 JAVA_HOME 설정 필요"
    exit 1
}
Write-Host "[build] JAVA_HOME = $env:JAVA_HOME"

# 3) Maven 실행파일 (BUILD_MVN_CMD 없으면 프로젝트 mvnw.cmd)
if ($BUILD_MVN_CMD) { $mvn = $BUILD_MVN_CMD } else { $mvn = Join-Path $projectDir 'mvnw.cmd' }
if (-not (Test-Path $mvn)) { Write-Error "Maven 실행파일 없음: $mvn"; exit 1 }

# 4) goals 기본값
if (-not $Goals -or $Goals.Count -eq 0) { $Goals = @('clean', 'package') }

# 5) 인자 조립 (배열 splatting → PowerShell 인자쪼갬/따옴표 문제 없음)
$mvnArgs = @()
if ($BUILD_MVN_SETTINGS)   { $mvnArgs += @('-s', $BUILD_MVN_SETTINGS) }
if ($BUILD_MVN_EXTRA_ARGS) { $mvnArgs += $BUILD_MVN_EXTRA_ARGS }
$mvnArgs += $Goals

Write-Host "[build] mvn  = $mvn"
Write-Host "[build] args = $($mvnArgs -join ' ')"
Write-Host "[build] cwd  = $projectDir"
Write-Host ("-" * 60)

Push-Location $projectDir
try {
    & $mvn @mvnArgs
    $code = $LASTEXITCODE
} finally {
    Pop-Location
}

Write-Host ("-" * 60)
if ($code -ne 0) { Write-Error "[build] FAILED (exit $code)"; exit $code }
Write-Host "[build] SUCCESS"
