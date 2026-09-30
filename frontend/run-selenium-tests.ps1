param(
    [switch]$ResetCredentials,
    [ValidateSet('test:e2e', 'test:ai-quiz', 'test:resource-upload', 'test:community-qa')]
    [string]$TestScript = 'test:e2e'
)

$ErrorActionPreference = 'Stop'

$credentialPath = Join-Path $env:LOCALAPPDATA 'TeachQuest\selenium-credentials.xml'
if ($ResetCredentials -and (Test-Path $credentialPath)) {
    Remove-Item $credentialPath -Force
}

if (Test-Path $credentialPath) {
    $credential = Import-Clixml -Path $credentialPath
}
else {
    $email = Read-Host 'TeachQuest student email'
    $securePassword = Read-Host 'TeachQuest password' -AsSecureString
    $credential = [System.Management.Automation.PSCredential]::new($email.Trim(), $securePassword)
    $credentialDirectory = Split-Path $credentialPath
    New-Item -ItemType Directory -Path $credentialDirectory -Force | Out-Null
    $credential | Export-Clixml -Path $credentialPath
    Write-Host 'Credentials saved encrypted for this Windows account.'
}

$passwordPointer = [IntPtr]::Zero
$testExitCode = 1

try {
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($credential.Password)
    $env:TEACHQUEST_TEST_EMAIL = $credential.UserName
    $env:TEACHQUEST_TEST_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)

    npm run $TestScript
    $testExitCode = $LASTEXITCODE
}
finally {
    if ($passwordPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
    $credential.Password.Dispose()
    Remove-Item Env:TEACHQUEST_TEST_EMAIL -ErrorAction SilentlyContinue
    Remove-Item Env:TEACHQUEST_TEST_PASSWORD -ErrorAction SilentlyContinue
}

exit $testExitCode