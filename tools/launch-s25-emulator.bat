@echo off
setlocal

set "ANDROID_SDK_ROOT=C:\Users\Brycter\AppData\Local\Android\Sdk"
set "ANDROID_HOME=%ANDROID_SDK_ROOT%"
set "ANDROID_AVD_HOME=C:\Users\Brycter\.android\avd"
set "ANDROID_USER_HOME=C:\Users\Brycter\.android"
set "AVD_NAME=ServiceOps_S25_Ultra"
set "APK_PATH=%~dp0..\app\build\outputs\apk\debug\app-debug.apk"

if not exist "%ANDROID_SDK_ROOT%\emulator\emulator.exe" (
  echo Android Emulator was not found at %ANDROID_SDK_ROOT%.
  pause
  exit /b 1
)

if not exist "%APK_PATH%" (
  echo The debug APK was not found. Build the app first with gradlew.bat assembleDebug.
  pause
  exit /b 1
)

echo Starting %AVD_NAME%...
start "Service Ops S25 Ultra" "%ANDROID_SDK_ROOT%\emulator\emulator.exe" ^
  -avd "%AVD_NAME%" ^
  -no-snapshot-load ^
  -feature -BluetoothEmulation ^
  -feature -Uwb

echo Waiting for Android to start...
"%ANDROID_SDK_ROOT%\platform-tools\adb.exe" wait-for-device

:wait_for_boot
for /f "usebackq delims=" %%A in (`"%ANDROID_SDK_ROOT%\platform-tools\adb.exe" shell getprop sys.boot_completed 2^>nul`) do set "BOOT_COMPLETE=%%A"
if not "%BOOT_COMPLETE%"=="1" (
  timeout /t 2 /nobreak >nul
  goto wait_for_boot
)

echo Installing Service Ops...
"%ANDROID_SDK_ROOT%\platform-tools\adb.exe" install -r "%APK_PATH%"
if errorlevel 1 (
  echo APK installation failed.
  pause
  exit /b 1
)

echo Opening Service Ops...
"%ANDROID_SDK_ROOT%\platform-tools\adb.exe" shell am start -n com.serviceops.mobile/.MainActivity
echo Ready.
endlocal
