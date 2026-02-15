@echo off
echo ========================================
echo Autocrypt ADB Setup for Windows
echo ========================================
echo.
echo This script will grant Autocrypt special permissions to work on Samsung Knox devices.
echo.
echo Prerequisites:
echo   1. ADB (Android Debug Bridge) must be installed
echo   2. Your Samsung phone must be connected via USB
echo   3. USB Debugging must be enabled on your phone
echo   4. You must accept the USB debugging prompt on your phone
echo.
pause

echo.
echo Checking ADB connection...
adb devices

echo.
echo If you see your device listed above, press any key to continue...
pause

echo.
echo Granting WRITE_SECURE_SETTINGS permission to Autocrypt...
adb shell pm grant com.autocrypt android.permission.WRITE_SECURE_SETTINGS

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo SUCCESS! Permission granted.
    echo ========================================
    echo.
    echo You can now disconnect your phone from the PC.
    echo Open Autocrypt app and tap "Test Permission" to verify.
    echo.
) else (
    echo.
    echo ========================================
    echo ERROR: Failed to grant permission
    echo ========================================
    echo.
    echo Troubleshooting:
    echo   1. Make sure ADB is installed and in your PATH
    echo   2. Verify your phone is connected (run: adb devices)
    echo   3. Accept USB debugging prompt on your phone
    echo   4. Make sure Autocrypt is installed on your phone
    echo.
)

pause
