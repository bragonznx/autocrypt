package com.autocrypt

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    companion object {
        private const val TAG = "OverlayService"
        const val CHANNEL_ID = "autocrypt_channel"
        const val NOTIFICATION_ID = 1
        const val EXTRA_REPETITIONS = "repetitions"
        const val EXTRA_CYCLE_DELAY = "cycle_delay"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var recordButton: ImageButton
    private lateinit var playButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var statusText: TextView
    private lateinit var countdownText: TextView

    private lateinit var clickRecorder: ClickRecorder
    private var isRecording = false
    private var isPlaying = false
    private var repetitions = 1
    private var cycleDelay = 1000L

    private var recordingOverlay: View? = null
    private val handler = Handler(Looper.getMainLooper())

    private val playbackReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AutoClickAccessibilityService.ACTION_PLAYBACK_STATE_CHANGED) {
                val playing = intent.getBooleanExtra(AutoClickAccessibilityService.EXTRA_IS_PLAYING, false)
                val currentRep = intent.getIntExtra(AutoClickAccessibilityService.EXTRA_CURRENT_REP, 0)
                val totalReps = intent.getIntExtra(AutoClickAccessibilityService.EXTRA_TOTAL_REPS, 1)
                updatePlaybackUI(playing, currentRep, totalReps)
            }
        }
    }

    private val countdownReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AutoClickAccessibilityService.ACTION_COUNTDOWN_UPDATE) {
                val seconds = intent.getIntExtra(AutoClickAccessibilityService.EXTRA_COUNTDOWN_SECONDS, -1)
                updateCountdownUI(seconds)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "OverlayService onCreate")
        clickRecorder = ClickRecorder.getInstance(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        setupOverlay()
        registerReceiver()
        loadSavedClicks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            repetitions = it.getIntExtra(EXTRA_REPETITIONS, 1)
            cycleDelay = it.getLongExtra(EXTRA_CYCLE_DELAY, 1000L)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(playbackReceiver) } catch (e: Exception) {}
        try { unregisterReceiver(countdownReceiver) } catch (e: Exception) {}
        removeRecordingOverlay()
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }

    private fun registerReceiver() {
        val playbackFilter = IntentFilter(AutoClickAccessibilityService.ACTION_PLAYBACK_STATE_CHANGED)
        val countdownFilter = IntentFilter(AutoClickAccessibilityService.ACTION_COUNTDOWN_UPDATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(playbackReceiver, playbackFilter, RECEIVER_NOT_EXPORTED)
            registerReceiver(countdownReceiver, countdownFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(playbackReceiver, playbackFilter)
            registerReceiver(countdownReceiver, countdownFilter)
        }
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun setupOverlay() {
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_controls, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL  // RIGHT side
            x = 0
            y = 0
        }

        recordButton = overlayView.findViewById(R.id.btnRecord)
        playButton = overlayView.findViewById(R.id.btnPlay)
        settingsButton = overlayView.findViewById(R.id.btnSettings)
        statusText = overlayView.findViewById(R.id.tvStatus)
        countdownText = overlayView.findViewById(R.id.tvCountdown)

        recordButton.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                v.performClick()
                toggleRecording()
            }
            true
        }

        recordButton.setOnLongClickListener {
            clearRecording()
            true
        }

        playButton.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                v.performClick()
                handlePlayClick()
            }
            true
        }

        settingsButton.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                v.performClick()
                openSettings()
            }
            true
        }

        windowManager.addView(overlayView, params)
        updateStatusText()
    }

    private fun toggleRecording() {
        if (isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        val service = AutoClickAccessibilityService.instance
        if (service == null) {
            Toast.makeText(this, "Active le service d'accessibilité!", Toast.LENGTH_LONG).show()
            startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            return
        }

        isRecording = true
        clickRecorder.startRecording()

        recordButton.alpha = 0.5f
        playButton.visibility = View.GONE
        settingsButton.visibility = View.GONE
        statusText.text = "0"
        statusText.setTextColor(0xFFFF0000.toInt())
        statusText.setBackgroundColor(0x33FF0000)
        statusText.visibility = View.VISIBLE

        createRecordingOverlay()
        Toast.makeText(this, "Enregistrement... Tape sur l'écran. REC pour terminer.", Toast.LENGTH_LONG).show()
    }

    private fun stopRecording() {
        isRecording = false
        clickRecorder.stopRecording()
        removeRecordingOverlay()

        recordButton.alpha = 1.0f
        playButton.visibility = View.VISIBLE
        settingsButton.visibility = View.VISIBLE

        val count = clickRecorder.getClickCount()
        if (count > 0) {
            statusText.text = "$count"
            statusText.setTextColor(0xFF00FF00.toInt())
            statusText.setBackgroundColor(0x3300FF00)
            playButton.isEnabled = true
            playButton.alpha = 1.0f
            Toast.makeText(this, "$count clics enregistrés!", Toast.LENGTH_SHORT).show()
        } else {
            statusText.visibility = View.GONE
            playButton.isEnabled = false
            playButton.alpha = 0.3f
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createRecordingOverlay() {
        val overlay = View(this).apply {
            setBackgroundColor(0x20000000)  // Very light tint so user sees they're recording
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        overlay.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN && isRecording) {
                val x = event.rawX
                val y = event.rawY

                // Record the click
                clickRecorder.recordClick(x, y)
                val count = clickRecorder.getClickCount()
                statusText.text = "$count"

                Log.i(TAG, "Recorded #$count at ($x, $y)")

                // Hide overlay temporarily so the click can reach the game
                overlay.visibility = View.INVISIBLE

                // Forward the click to the game
                AutoClickAccessibilityService.instance?.performSingleClick(x, y) {
                    handler.postDelayed({
                        // Show overlay again after click is done
                        overlay.visibility = View.VISIBLE
                        Toast.makeText(this, "#$count", Toast.LENGTH_SHORT).show()
                    }, 100)
                }
            }
            true
        }

        recordingOverlay = overlay
        windowManager.addView(overlay, params)

        // Bring control panel to front
        bringControlsToFront()
    }

    private fun bringControlsToFront() {
        try {
            val currentParams = overlayView.layoutParams as WindowManager.LayoutParams
            windowManager.removeView(overlayView)
            windowManager.addView(overlayView, currentParams)
        } catch (e: Exception) {
            Log.e(TAG, "Error bringing controls to front", e)
        }
    }

    private fun removeRecordingOverlay() {
        recordingOverlay?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
            recordingOverlay = null
        }
    }

    private fun clearRecording() {
        clickRecorder.clearClicks()
        statusText.visibility = View.GONE
        playButton.isEnabled = false
        playButton.alpha = 0.3f
        Toast.makeText(this, "Enregistrement effacé", Toast.LENGTH_SHORT).show()
    }

    private fun loadSavedClicks() {
        val clicks = clickRecorder.loadClicks()
        if (clicks.isNotEmpty()) {
            statusText.text = "${clicks.size}"
            statusText.setTextColor(0xFF00FF00.toInt())
            statusText.setBackgroundColor(0x3300FF00)
            statusText.visibility = View.VISIBLE
            playButton.isEnabled = true
            playButton.alpha = 1.0f
        }
    }

    private fun updateStatusText() {
        val count = clickRecorder.getClickCount()
        if (count > 0) {
            statusText.text = "$count"
            statusText.setTextColor(0xFF00FF00.toInt())
            statusText.setBackgroundColor(0x3300FF00)
            statusText.visibility = View.VISIBLE
            playButton.isEnabled = true
            playButton.alpha = 1.0f
        } else {
            statusText.visibility = View.GONE
            playButton.isEnabled = false
            playButton.alpha = 0.3f
        }
    }

    private fun handlePlayClick() {
        if (isPlaying) {
            AutoClickAccessibilityService.instance?.stopPlaying()
            return
        }

        val service = AutoClickAccessibilityService.instance
        if (service == null) {
            Toast.makeText(this, "Active le service d'accessibilité!", Toast.LENGTH_LONG).show()
            startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            return
        }

        val clicks = clickRecorder.loadClicks()
        if (clicks.isEmpty()) {
            Toast.makeText(this, "Aucun clic enregistré!", Toast.LENGTH_SHORT).show()
            return
        }

        service.startPlaying(clicks, repetitions, cycleDelay)
    }

    private fun updatePlaybackUI(playing: Boolean, currentRep: Int, totalReps: Int) {
        isPlaying = playing
        if (playing) {
            playButton.setImageResource(R.drawable.ic_pause)
            recordButton.isEnabled = false
            recordButton.alpha = 0.3f
            statusText.text = "${currentRep + 1}/$totalReps"
            statusText.setTextColor(0xFF00AAFF.toInt())
            statusText.setBackgroundColor(0x3300AAFF)
            statusText.visibility = View.VISIBLE
        } else {
            playButton.setImageResource(R.drawable.ic_play)
            recordButton.isEnabled = true
            recordButton.alpha = 1.0f
            countdownText.visibility = View.GONE
            updateStatusText()
        }
    }

    private fun updateCountdownUI(seconds: Int) {
        if (seconds < 0) {
            countdownText.visibility = View.GONE
        } else {
            countdownText.text = "${seconds}s"
            countdownText.visibility = View.VISIBLE
        }
    }

    private fun openSettings() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Autocrypt", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Autocrypt")
            .setContentText("Auto-clicker actif")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pi)
            .build()
    }
}
