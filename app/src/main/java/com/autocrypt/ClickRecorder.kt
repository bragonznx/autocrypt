package com.autocrypt

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class ClickEvent(
    val x: Float,
    val y: Float,
    val delayMs: Long
)

class ClickRecorder(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("autocrypt_prefs", Context.MODE_PRIVATE)
    private val clicks = mutableListOf<ClickEvent>()
    private var lastClickTime: Long = 0
    private var isRecording = false

    fun startRecording() {
        clicks.clear()
        lastClickTime = System.currentTimeMillis()
        isRecording = true
    }

    fun stopRecording() {
        isRecording = false
        saveClicks()
    }

    fun recordClick(x: Float, y: Float) {
        if (!isRecording) return

        val now = System.currentTimeMillis()
        val delay = if (clicks.isEmpty()) 0L else now - lastClickTime
        clicks.add(ClickEvent(x, y, delay))
        lastClickTime = now
    }

    fun getClicks(): List<ClickEvent> = clicks.toList()

    fun loadClicks(): List<ClickEvent> {
        val json = prefs.getString("recorded_clicks", null) ?: return emptyList()
        val result = mutableListOf<ClickEvent>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(ClickEvent(
                    obj.getDouble("x").toFloat(),
                    obj.getDouble("y").toFloat(),
                    obj.getLong("delay")
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        clicks.clear()
        clicks.addAll(result)
        return result
    }

    private fun saveClicks() {
        val array = JSONArray()
        clicks.forEach { click ->
            val obj = JSONObject()
            obj.put("x", click.x.toDouble())
            obj.put("y", click.y.toDouble())
            obj.put("delay", click.delayMs)
            array.put(obj)
        }
        prefs.edit().putString("recorded_clicks", array.toString()).apply()
    }

    fun hasRecording(): Boolean {
        return prefs.getString("recorded_clicks", null)?.let {
            JSONArray(it).length() > 0
        } ?: false
    }

    fun getClickCount(): Int = clicks.size

    fun saveClicksDirect(clickList: List<ClickEvent>) {
        clicks.clear()
        clicks.addAll(clickList)
        saveClicks()
    }

    fun clearClicks() {
        clicks.clear()
        prefs.edit().remove("recorded_clicks").apply()
    }

    companion object {
        @Volatile
        private var instance: ClickRecorder? = null

        fun getInstance(context: Context): ClickRecorder {
            return instance ?: synchronized(this) {
                instance ?: ClickRecorder(context.applicationContext).also { instance = it }
            }
        }
    }
}
