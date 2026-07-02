$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$EnvFile = Join-Path $ScriptDir ".env"
$OutputFile = Join-Path $ScriptDir "careermate_backup.sql"

function Import-DotEnv {
    param([string] $Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        return
    }

    foreach ($RawLine in Get-Content -LiteralPath $Path) {
        $Line = $RawLine.Trim()
        if (-not $Line -or $Line.StartsWith("#")) {
            continue
        }

        $Parts = $Line -split "=", 2
        if ($Parts.Count -ne 2) {
            continue
        }

        $Name = $Parts[0].Trim()
        $Value = $Parts[1].Trim()
        $Value = $Value.Trim('"').Trim("'")

        if ($Name) {
            [Environment]::SetEnvironmentVariable($Name, $Value, "Process")
        }
    }
}

function Require-Env {
    param([string] $Name)

    $Value = [Environment]::GetEnvironmentVariable($Name, "Process")
    if ([string]::IsNullOrWhiteSpace($Value)) {
        throw "Missing required environment variable: $Name"
    }

    return $Value
}

function Resolve-PostgresTool {
    param([string] $ToolName)

    $Command = Get-Command $ToolName -ErrorAction SilentlyContinue
    if ($Command) {
        return $Command.Source
    }

    $CommonPath = Get-ChildItem -LiteralPath "C:\Program Files\PostgreSQL" -Recurse -Filter "$ToolName.exe" -ErrorAction SilentlyContinue |
        Select-Object -First 1

    if ($CommonPath) {
        return $CommonPath.FullName
    }

    throw @"
Cannot find $ToolName.

Please install PostgreSQL client tools, then reopen PowerShell and run:
  $ToolName --version

On Windows, $ToolName is usually in:
  C:\Program Files\PostgreSQL\<version>\bin

If PostgreSQL is already installed, add its bin folder to PATH before running this script.
"@
}

Import-DotEnv -Path $EnvFile
$PgDump = Resolve-PostgresTool "pg_dump"

$HostName = Require-Env "OLD_DB_HOST"
$Port = Require-Env "OLD_DB_PORT"
$DbName = Require-Env "OLD_DB_NAME"
$User = Require-Env "OLD_DB_USER"
$Password = Require-Env "OLD_DB_PASSWORD"
$ConnectionString = "host=$HostName port=$Port dbname=$DbName sslmode=require"

$PreviousPassword = $env:PGPASSWORD

try {
    $env:PGPASSWORD = $Password

    Write-Host "Exporting old PostgreSQL database to: $OutputFile"

    & $PgDump `
        --username $User `
        --dbname $ConnectionString `
        --format plain `
        --no-owner `
        --no-acl `
        --file $OutputFile

    if ($LASTEXITCODE -ne 0) {
        throw "pg_dump failed with exit code $LASTEXITCODE"
    }

    Write-Host "Export completed successfully."
}
finally {
    $env:PGPASSWORD = $PreviousPassword
}
