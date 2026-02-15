package com.autocrypt

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AdbSetupActivity : AppCompatActivity() {

    private lateinit var tvInstructions: TextView
    private lateinit var tvAdbCommand: TextView
    private lateinit var btnCopyCommand: Button
    private lateinit var btnOpenDeveloperSettings: Button
    private lateinit var btnTestPermission: Button
    private lateinit var btnContinue: Button
    private lateinit var tvPermissionStatus: TextView

    private val adbCommand = """
        adb shell pm grant com.autocrypt android.permission.WRITE_SECURE_SETTINGS
    """.trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adb_setup)

        initViews()
        setupListeners()
        checkPermissionStatus()
    }

    override fun onResume() {
        super.onResume()
        checkPermissionStatus()
    }

    private fun initViews() {
        tvInstructions = findViewById(R.id.tvAdbInstructions)
        tvAdbCommand = findViewById(R.id.tvAdbCommand)
        btnCopyCommand = findViewById(R.id.btnCopyCommand)
        btnOpenDeveloperSettings = findViewById(R.id.btnOpenDeveloperSettings)
        btnTestPermission = findViewById(R.id.btnTestPermission)
        btnContinue = findViewById(R.id.btnContinue)
        tvPermissionStatus = findViewById(R.id.tvPermissionStatus)

        tvAdbCommand.text = adbCommand
    }

    private fun setupListeners() {
        btnCopyCommand.setOnClickListener {
            copyCommandToClipboard()
        }

        btnOpenDeveloperSettings.setOnClickListener {
            openDeveloperSettings()
        }

        btnTestPermission.setOnClickListener {
            checkPermissionStatus()
        }

        btnContinue.setOnClickListener {
            if (hasWriteSecureSettings()) {
                // Permission granted, return to MainActivity
                finish()
                Toast.makeText(
                    this,
                    "ADB setup complete! You can now use Autocrypt.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Please complete ADB setup first",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun copyCommandToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("ADB Command", adbCommand)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Command copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    private fun openDeveloperSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            startActivity(intent)
            Toast.makeText(
                this,
                "Enable USB Debugging and connect to PC",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Could not open Developer Settings. Please enable it manually in Settings > About Phone > tap Build Number 7 times",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun checkPermissionStatus() {
        val hasPermission = hasWriteSecureSettings()

        tvPermissionStatus.text = if (hasPermission) {
            "✓ ADB Permission Granted"
        } else {
            "✗ ADB Permission Not Granted"
        }

        tvPermissionStatus.setTextColor(
            if (hasPermission) 0xFF00AA00.toInt() else 0xFFAA0000.toInt()
        )

        btnContinue.isEnabled = hasPermission
    }

    private fun hasWriteSecureSettings(): Boolean {
        return try {
            Settings.Secure.putInt(contentResolver, "autocrypt_test", 1)
            Settings.Secure.getInt(contentResolver, "autocrypt_test", 0)
            // Clean up test setting
            Settings.Secure.putString(contentResolver, "autocrypt_test", null)
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
