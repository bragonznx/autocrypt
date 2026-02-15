#!/bin/bash

echo "========================================"
echo "Autocrypt ADB Setup for Mac/Linux"
echo "========================================"
echo ""
echo "This script will grant Autocrypt special permissions to work on Samsung Knox devices."
echo ""
echo "Prerequisites:"
echo "  1. ADB (Android Debug Bridge) must be installed"
echo "  2. Your Samsung phone must be connected via USB"
echo "  3. USB Debugging must be enabled on your phone"
echo "  4. You must accept the USB debugging prompt on your phone"
echo ""
read -p "Press Enter to continue..."

echo ""
echo "Checking ADB connection..."
adb devices

echo ""
read -p "If you see your device listed above, press Enter to continue..."

echo ""
echo "Step 1/3: Granting WRITE_SECURE_SETTINGS permission..."
adb shell pm grant com.autocrypt android.permission.WRITE_SECURE_SETTINGS

if [ $? -ne 0 ]; then
    echo "ERROR: Failed to grant permission"
    exit 1
fi

echo ""
echo "Step 2/3: Enabling accessibility service..."
adb shell settings put secure enabled_accessibility_services com.autocrypt/.AutoClickAccessibilityService

if [ $? -ne 0 ]; then
    echo "ERROR: Failed to enable accessibility service"
    exit 1
fi

echo ""
echo "Step 3/3: Activating accessibility globally..."
adb shell settings put secure accessibility_enabled 1

if [ $? -ne 0 ]; then
    echo "ERROR: Failed to activate accessibility"
    exit 1
fi

echo ""
echo "========================================"
echo "SUCCESS! Knox compatibility setup complete."
echo "========================================"
echo ""
echo "Autocrypt is now configured to work on your Samsung device:"
echo "  - WRITE_SECURE_SETTINGS permission: Granted"
echo "  - Accessibility service: Enabled"
echo "  - Ready to use!"
echo ""
echo "You can now disconnect your phone from the PC."
echo "Open Autocrypt app - it should work normally now."
echo ""

read -p "Press Enter to exit..."
