#!/usr/bin/env powershell
$env:GIT_EDITOR = 'true'
$env:GIT_SEQUENCE_EDITOR = 'true'
& git push origin master --force 2>&1 | Out-Host
Write-Output "Push completed"
