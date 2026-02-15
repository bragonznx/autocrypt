package com.autocrypt

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var btnOverlayPermission: Button
    private lateinit var btnAccessibilityPermission: Button
    private lateinit var btnStartService: Button
    private lateinit var btnStopService: Button
    private lateinit var etRepetitions: EditText
    private lateinit var etCycleDelay: EditText
    private lateinit var tvOverlayStatus: TextView
    private lateinit var tvAccessibilityStatus: TextView
    private lateinit var tvGithubLink: TextView
    private var samsungDialogShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        checkSamsungDevice()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()

        // Check Samsung device again if dialog hasn't been shown
        if (!samsungDialogShown) {
            checkSamsungDevice()
        }
    }

    private fun initViews() {
        btnOverlayPermission = findViewById(R.id.btnOverlayPermission)
        btnAccessibilityPermission = findViewById(R.id.btnAccessibilityPermission)
        btnStartService = findViewById(R.id.btnStartService)
        btnStopService = findViewById(R.id.btnStopService)
        etRepetitions = findViewById(R.id.etRepetitions)
        etCycleDelay = findViewById(R.id.etCycleDelay)
        tvOverlayStatus = findViewById(R.id.tvOverlayStatus)
        tvAccessibilityStatus = findViewById(R.id.tvAccessibilityStatus)

        tvGithubLink = findViewById(R.id.tvGithubLink)

        etRepetitions.setText("10")
        etCycleDelay.setText("50")  // Default 50 seconds
    }

    private fun setupListeners() {
        btnOverlayPermission.setOnClickListener {
            requestOverlayPermission()
        }

        btnAccessibilityPermission.setOnClickListener {
            openAccessibilitySettings()
        }

        btnStartService.setOnClickListener {
            startOverlayService()
        }

        btnStopService.setOnClickListener {
            stopOverlayService()
        }

        tvGithubLink.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/bragonznx/autocrypt"))
            startActivity(intent)
        }
    }

    private fun updatePermissionStatus() {
        val hasOverlay = Settings.canDrawOverlays(this)
        val hasAccessibility = AutoClickAccessibilityService.isServiceEnabled()

        tvOverlayStatus.text = if (hasOverlay) "Granted" else "Not granted"
        tvOverlayStatus.setTextColor(
            if (hasOverlay) 0xFF00AA00.toInt() else 0xFFAA0000.toInt()
        )

        tvAccessibilityStatus.text = if (hasAccessibility) "Enabled" else "Not enabled"
        tvAccessibilityStatus.setTextColor(
            if (hasAccessibility) 0xFF00AA00.toInt() else 0xFFAA0000.toInt()
        )

        btnStartService.isEnabled = hasOverlay && hasAccessibility
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            Toast.makeText(this, "Overlay permission already granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(
            this,
            "Find 'Autocrypt' and enable it",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun startOverlayService() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please grant overlay permission first", Toast.LENGTH_SHORT).show()
            return
        }

        if (!AutoClickAccessibilityService.isServiceEnabled()) {
            Toast.makeText(this, "Please enable Accessibility Service first", Toast.LENGTH_SHORT).show()
            return
        }

        val repetitions = etRepetitions.text.toString().toIntOrNull() ?: 10
        val cycleDelaySeconds = etCycleDelay.text.toString().toLongOrNull() ?: 50L
        val cycleDelay = cycleDelaySeconds * 1000  // Convert seconds to milliseconds

        val intent = Intent(this, OverlayService::class.java).apply {
            putExtra(OverlayService.EXTRA_REPETITIONS, repetitions)
            putExtra(OverlayService.EXTRA_CYCLE_DELAY, cycleDelay)
        }
        startForegroundService(intent)

        Toast.makeText(this, "Overlay started! Switch to your game.", Toast.LENGTH_SHORT).show()
    }

    private fun stopOverlayService() {
        stopService(Intent(this, OverlayService::class.java))
        Toast.makeText(this, "Overlay stopped", Toast.LENGTH_SHORT).show()
    }

    private fun checkSamsungDevice() {
        // Check if this is a Samsung device
        if (DeviceUtils.isSamsungDevice()) {
            // Check if ADB permission already granted
            if (!hasWriteSecureSettings()) {
                showSamsungKnoxWarning()
            } else {
                // User has already completed ADB setup
                Toast.makeText(
                    this,
                    "Samsung device detected - ADB setup complete ✓",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showSamsungKnoxWarning() {
        if (samsungDialogShown) return
        samsungDialogShown = true

        AlertDialog.Builder(this)
            .setTitle("⚠️ Samsung Knox Detected")
            .setMessage(
                "Samsung Knox security blocks standard accessibility permissions.\n\n" +
                "To use Autocrypt on your Samsung device, you need to complete a one-time ADB setup:\n\n" +
                "• Connect phone to PC\n" +
                "• Run a simple ADB command\n" +
                "• Takes about 5 minutes\n\n" +
                "Would you like to see ADB setup instructions?"
            )
            .setPositiveButton("Setup ADB") { _, _ ->
                // Open ADB setup activity
                startActivity(Intent(this, AdbSetupActivity::class.java))
            }
            .setNegativeButton("Try Anyway") { _, _ ->
                Toast.makeText(
                    this,
                    "Note: Accessibility permissions may be blocked by Knox",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setNeutralButton("More Info") { _, _ ->
                showSamsungMoreInfo()
            }
            .setCancelable(true)
            .show()
    }

    private fun showSamsungMoreInfo() {
        AlertDialog.Builder(this)
            .setTitle("Why is ADB needed?")
            .setMessage(
                "Samsung Knox is a security platform that protects your device by blocking apps from simulating touches and controlling the screen.\n\n" +
                "Autocrypt is an auto-clicker that needs these permissions to work. Without ADB setup, Knox will block the required permissions.\n\n" +
                "The ADB method grants special permissions that allow Autocrypt to work while maintaining your device security.\n\n" +
                "Alternative: Use a non-Samsung device for auto-clicking apps."
            )
            .setPositiveButton("Setup ADB") { _, _ ->
                startActivity(Intent(this, AdbSetupActivity::class.java))
            }
            .setNegativeButton("Close") { _, _ -> }
            .show()
    }

    private fun hasWriteSecureSettings(): Boolean {
        return try {
            Settings.Secure.putInt(contentResolver, "autocrypt_test", 1)
            Settings.Secure.getInt(contentResolver, "autocrypt_test", 0)
            Settings.Secure.putString(contentResolver, "autocrypt_test", null)
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
