@echo off
cd /d "c:\Users\admin\AppData\Roaming\PrismLauncher\instances\NotPack - Performance\minecraft\Honed"
set GIT_EDITOR=
set GIT_SEQUENCE_EDITOR=
set GIT_TERMINAL_PROMPT=0
REM Fetch latest
git fetch origin master >nul 2>&1
REM Merge origin/master into local
git merge origin/master -m "Merge origin/master" 2>&1
REM Push to origin
git push origin master 2>&1
echo Done
pause
