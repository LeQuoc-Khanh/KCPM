$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$EnvFile = Join-Path $ScriptDir ".env"
$InputFile = Join-Path $ScriptDir "careermate_backup.sql"

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
$Psql = Resolve-PostgresTool "psql"

if (-not (Test-Path -LiteralPath $InputFile)) {
    throw "Backup file not found: $InputFile"
}

$HostName = Require-Env "SUPABASE_DB_HOST"
$Port = Require-Env "SUPABASE_DB_PORT"
$DbName = Require-Env "SUPABASE_DB_NAME"
$User = Require-Env "SUPABASE_DB_USER"
$Password = Require-Env "SUPABASE_DB_PASSWORD"
$ConnectionString = "host=$HostName port=$Port dbname=$DbName sslmode=require"

$PreviousPassword = $env:PGPASSWORD

try {
    $env:PGPASSWORD = $Password

    Write-Host "Importing backup into Supabase database: $DbName"

    & $Psql `
        --username $User `
        --dbname $ConnectionString `
        --set ON_ERROR_STOP=on `
        --file $InputFile

    if ($LASTEXITCODE -ne 0) {
        throw "psql failed with exit code $LASTEXITCODE"
    }

    Write-Host "Import completed successfully."
}
finally {
    $env:PGPASSWORD = $PreviousPassword
}
