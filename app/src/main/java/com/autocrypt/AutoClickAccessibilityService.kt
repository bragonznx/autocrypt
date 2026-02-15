package com.autocrypt

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class AutoClickAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var isPlaying = false
    private var currentClickIndex = 0
    private var currentRepetition = 0
    private var totalRepetitions = 1
    private var delayBetweenCycles = 1000L
    private var clicks = listOf<ClickEvent>()
    private var countdownRunnable: Runnable? = null

    // Callback for UI updates
    var onPlaybackStateChanged: ((Boolean, Int, Int) -> Unit)? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "=== ACCESSIBILITY SERVICE CONNECTED ===")
        handler.post {
            Toast.makeText(this, "Autocrypt Accessibility: READY", Toast.LENGTH_LONG).show()
        }
    }

    // Recording mode
    private var recordingCallback: ((Float, Float) -> Unit)? = null

    fun startRecordingTouches(callback: (Float, Float) -> Unit) {
        recordingCallback = callback
        Log.i(TAG, "Started recording touches via AccessibilityService")
    }

    fun stopRecordingTouches() {
        recordingCallback = null
        Log.i(TAG, "Stopped recording touches")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not needed for click events - we use onGesture for touch detection
    }

    override fun onInterrupt() {
        Log.w(TAG, "Service interrupted")
        stopPlaying()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "=== ACCESSIBILITY SERVICE DESTROYED ===")
        instance = null
        stopPlaying()
    }

    fun startPlaying(clickList: List<ClickEvent>, repetitions: Int, cycleDelay: Long) {
        Log.i(TAG, "startPlaying called: ${clickList.size} clicks, $repetitions reps, ${cycleDelay}ms delay")

        if (isPlaying) {
            Log.w(TAG, "Already playing, ignoring")
            return
        }

        if (clickList.isEmpty()) {
            Log.e(TAG, "No clicks to play!")
            handler.post {
                Toast.makeText(this, "No clicks recorded!", Toast.LENGTH_SHORT).show()
            }
            return
        }

        clicks = clickList
        totalRepetitions = repetitions
        delayBetweenCycles = cycleDelay
        currentClickIndex = 0
        currentRepetition = 0
        isPlaying = true

        // Notify state change
        notifyStateChanged()

        handler.post {
            Toast.makeText(this, "Playing ${clicks.size} clicks x $totalRepetitions", Toast.LENGTH_SHORT).show()
        }

        // Start immediately
        executeNextClick()
    }

    fun stopPlaying() {
        if (!isPlaying) return

        Log.i(TAG, "Stopping playback")
        isPlaying = false
        stopCountdown()
        handler.removeCallbacksAndMessages(null)
        notifyStateChanged()
    }

    private fun notifyStateChanged() {
        val intent = Intent(ACTION_PLAYBACK_STATE_CHANGED).apply {
            setPackage(packageName)
            putExtra(EXTRA_IS_PLAYING, isPlaying)
            putExtra(EXTRA_CURRENT_REP, currentRepetition)
            putExtra(EXTRA_TOTAL_REPS, totalRepetitions)
        }
        sendBroadcast(intent)
    }

    private fun startCountdown(totalMs: Long) {
        stopCountdown()
        var remainingSeconds = (totalMs / 1000).toInt()

        countdownRunnable = object : Runnable {
            override fun run() {
                if (!isPlaying || remainingSeconds <= 0) {
                    notifyCountdown(-1) // Hide countdown
                    if (isPlaying && remainingSeconds <= 0) {
                        executeNextClick()
                    }
                    return
                }

                notifyCountdown(remainingSeconds)
                remainingSeconds--
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(countdownRunnable!!)
    }

    private fun stopCountdown() {
        countdownRunnable?.let { handler.removeCallbacks(it) }
        countdownRunnable = null
        notifyCountdown(-1)
    }

    private fun notifyCountdown(seconds: Int) {
        val intent = Intent(ACTION_COUNTDOWN_UPDATE).apply {
            setPackage(packageName)
            putExtra(EXTRA_COUNTDOWN_SECONDS, seconds)
        }
        sendBroadcast(intent)
    }

    private fun executeNextClick() {
        if (!isPlaying) {
            Log.d(TAG, "Not playing anymore, stopping")
            return
        }

        // Check if current cycle is done
        if (currentClickIndex >= clicks.size) {
            currentRepetition++
            Log.i(TAG, "Cycle complete. Rep $currentRepetition/$totalRepetitions")

            if (currentRepetition >= totalRepetitions) {
                Log.i(TAG, "All repetitions complete!")
                handler.post {
                    Toast.makeText(this, "Done! $totalRepetitions cycles completed", Toast.LENGTH_SHORT).show()
                }
                isPlaying = false
                notifyStateChanged()
                return
            }

            // Reset for next cycle
            currentClickIndex = 0
            notifyStateChanged()

            // Wait before next cycle with countdown
            Log.d(TAG, "Waiting ${delayBetweenCycles}ms before next cycle")
            startCountdown(delayBetweenCycles)
            return
        }

        val click = clicks[currentClickIndex]
        val delay = if (currentClickIndex == 0) 0L else click.delayMs

        Log.d(TAG, "Click ${currentClickIndex + 1}/${clicks.size}: (${click.x}, ${click.y}) delay=${delay}ms")

        handler.postDelayed({
            if (isPlaying) {
                performClick(click.x, click.y)
                currentClickIndex++
                executeNextClick()
            }
        }, delay)
    }

    private fun performClick(x: Float, y: Float) {
        Log.d(TAG, "Performing click at ($x, $y)")

        val path = Path().apply {
            moveTo(x, y)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()

        val dispatched = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d(TAG, "Gesture completed at ($x, $y)")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.e(TAG, "Gesture CANCELLED at ($x, $y)")
            }
        }, null)

        if (!dispatched) {
            Log.e(TAG, "dispatchGesture returned FALSE at ($x, $y)")
        }
    }

    // Perform a single click immediately (used during recording to pass-through touches)
    fun performSingleClick(x: Float, y: Float, callback: (() -> Unit)? = null) {
        Log.d(TAG, "Performing single click at ($x, $y)")

        val path = Path().apply {
            moveTo(x, y)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d(TAG, "Single click completed at ($x, $y)")
                callback?.invoke()
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.e(TAG, "Single click CANCELLED at ($x, $y)")
                callback?.invoke()
            }
        }, null)
    }

    companion object {
        private const val TAG = "AutoClickService"

        const val ACTION_PLAYBACK_STATE_CHANGED = "com.autocrypt.PLAYBACK_STATE_CHANGED"
        const val ACTION_COUNTDOWN_UPDATE = "com.autocrypt.COUNTDOWN_UPDATE"
        const val EXTRA_IS_PLAYING = "is_playing"
        const val EXTRA_CURRENT_REP = "current_rep"
        const val EXTRA_TOTAL_REPS = "total_reps"
        const val EXTRA_COUNTDOWN_SECONDS = "countdown_seconds"

        @Volatile
        var instance: AutoClickAccessibilityService? = null
            private set

        fun isServiceEnabled(): Boolean = instance != null
    }
}
