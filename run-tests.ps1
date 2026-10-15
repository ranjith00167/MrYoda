<#
.SYNOPSIS
    Runs the MrYoda API test suite with correct UTF-8 console output.

.DESCRIPTION
    Fixes BOTH layers of the console "decoding" (mojibake) problem:

      Layer 1 (Java)  - handled by .mvn/jvm.config, which forces the Maven JVM
                        to file.encoding=UTF-8 instead of the Windows default
                        Cp1252. pom.xml's surefire <argLine> covers the forked
                        test JVM.

      Layer 2 (shell) - handled HERE. pwsh decodes native command output using
                        [Console]::OutputEncoding, which defaults to cp437 on
                        Windows. Correct UTF-8 bytes emitted by Java are then
                        read back as mojibake ("ΓÇö" instead of an em-dash).

    Symptoms this fixes:
      "[AIFailureAnalyzer] Skipping ù analyzerEnabled=false"  ->  "Skipping — ..."
      "? AUTH-04 PASSED ù ..."                               ->  "✔ AUTH-04 PASSED — ..."
      "TestNG 7.8.0 by CΘdric Beust"                         ->  "by Cédric Beust"
      "BACKEND GAP DETECTED [BUG-AUTH-001]        ?"         ->  proper box drawing

.PARAMETER Suite
    Suite XML file name inside test-suites\ (default: testng_all.xml).

.PARAMETER Args
    Any extra arguments forwarded to Maven (e.g. -Dtest=CryptoUtilTest).

.EXAMPLE
    .\run-tests.ps1
    .\run-tests.ps1 -Suite testng_otp_suite.xml
    .\run-tests.ps1 -Suite testng_otp_suite.xml -Args "-Dheadless=true"
    .\run-tests.ps1 -Args "-Dtest=CryptoUtilTest"
#>
[CmdletBinding()]
param(
    [string] $Suite = 'testng_all.xml',
    [string[]] $Args = @()
)

$ErrorActionPreference = 'Stop'

# ---------- LAYER 2: force the shell to speak UTF-8 ----------
# Required order: set $OutputEncoding (what we send TO native apps) and
# [Console]::OutputEncoding (how pwsh decodes native stdout/stderr).
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding             = [System.Text.Encoding]::UTF8

# Also set the process/console code page to 65001 so a plain `mvn ... > log.txt`
# redirect from this terminal stays UTF-8. Ignored if chcp is unavailable.
try { & chcp.com 65001 > $null 2>&1 } catch { }

# ---------- Sanity checks ----------
$suitePath = Join-Path $PSScriptRoot "test-suites\$Suite"
if (-not (Test-Path $suitePath)) {
    Write-Host "[run-tests] Suite not found: $suitePath" -ForegroundColor Red
    Write-Host "[run-tests] Available suites (test-suites\*.xml):" -ForegroundColor Yellow
    Get-ChildItem (Join-Path $PSScriptRoot 'test-suites') -Filter *.xml |
        Select-Object -ExpandProperty Name | ForEach-Object { "    $_" }
    exit 1
}

$mavenArgs = @('-B', 'test')
if ($Args -contains '-Dtest' -or ($Args -join ' ') -match '-Dtest=') {
    # Caller is running a specific class - do not force a suite XML.
} else {
    $mavenArgs += "-DsuiteXmlFile=test-suites/$Suite"
}
$mavenArgs += $Args

Write-Host ("[run-tests] Console encoding : {0}" -f [Console]::OutputEncoding.WebName) -ForegroundColor Cyan
Write-Host ("[run-tests] Maven JVM        : UTF-8 (via .mvn\jvm.config)") -ForegroundColor Cyan
Write-Host ("[run-tests] Command          : mvn {0}" -f ($mavenArgs -join ' ')) -ForegroundColor Cyan
Write-Host ('-' * 70)

Push-Location $PSScriptRoot
try {
    & mvn @mavenArgs
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
