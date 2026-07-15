@echo off
setlocal
rem ---- CustomBlocks white-ring fix: before/after sample tester ----
set "BASE=%~dp0"
set "JAVA=C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot\bin\java.exe"

if not exist "%JAVA%" (
  echo Could not find JDK 21 at:
  echo   %JAVA%
  echo Edit run_test.bat and set JAVA to your jdk-21 java.exe path.
  pause
  exit /b 1
)

echo Put your test images in the "samples" folder, then run this.
echo Results go to the "out" folder as NAME_COMPARE.png  (left = OLD, right = NEW fix).
echo.
set "TOL="
set /p "TOL=Tolerance 0-100 [default 50]: "
if "%TOL%"=="" set "TOL=50"

"%JAVA%" -cp "%BASE%engine" QSample "%BASE%." %TOL%

echo.
start "" "%BASE%out"
pause
