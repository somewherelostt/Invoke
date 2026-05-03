package com.invoke.android.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.invoke.android.service.InvokeAccessibilityService

/**
 * INVOKE main activity.
 * First launch → one-time onboarding (mic + accessibility).
 * After that → settings screen.
 * Composio is optional — hidden behind a single "Connect" button, asks once.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val PREFS = "invoke_prefs"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_MIC_GRANTED = "mic_granted"
        private const val KEY_ACCESSIBILITY_ENABLED = "accessibility_enabled"
        private const val KEY_COMPOSIO_CONNECTED = "composio_connected"
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE)

        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(16), dp(24), dp(16))
        }
        setContentView(container)

        if (prefs.getBoolean(KEY_ONBOARDING_DONE, false)) {
            showSettings()
        } else {
            showOnboarding()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh onboarding state when returning from permission screens
        if (!prefs.getBoolean(KEY_ONBOARDING_DONE, false)) {
            showOnboarding()
        }
    }

    // ─── Onboarding (one-time) ───

    private fun showOnboarding() {
        container.removeAllViews()

        val micGranted = checkMicPermission()
        val accEnabled = isAccessibilityEnabled()

        // Track what's done
        prefs.edit()
            .putBoolean(KEY_MIC_GRANTED, micGranted)
            .putBoolean(KEY_ACCESSIBILITY_ENABLED, accEnabled)
            .apply()

        // Title
        container += title("🔮 INVOKE Setup")

        if (!micGranted) {
            container += subtitle("Step 1: Allow Microphone")
            container += body("INVOKE needs your microphone to hear voice commands. Audio never leaves your device.")
            container += button("Grant Microphone Access") {
                requestMicPermission()
            }
            container += space()
        } else {
            container += checkRow("✅ Microphone enabled")
        }

        if (!accEnabled) {
            container += subtitle("Step 2: Enable Accessibility")
            container += body("INVOKE uses accessibility to insert text in any app. This is how it types for you.")
            container += body("Settings → Accessibility → INVOKE → Enable")
            container += button("Open Accessibility Settings") {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            container += space()
        } else {
            container += checkRow("✅ Accessibility enabled")
        }

        if (micGranted && accEnabled) {
            container += space()
            container += subtitle("✨ You're all set!")
            container += body("Tap the floating bubble in any app to start.\nTap to record, tap again to act.")
            container += body("Hold & drag to move the bubble. It snaps to the edge.")
            container += button("Start Using INVOKE") {
                prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
                showSettings()
            }
        }
    }

    // ─── Settings (after onboarding) ───

    private fun showSettings() {
        container.removeAllViews()

        container += title("🔮 INVOKE")

        // Status
        val sttReady = com.invoke.android.stt.SttEngine.isReady()
        container += checkRow(if (sttReady) "✅ Whisper STT ready" else "⏳ Whisper STT not loaded")

        val accEnabled = isAccessibilityEnabled()
        container += checkRow(if (accEnabled) "✅ Floating bubble active" else "⚠️ Accessibility disabled")

        container += space()

        // Ollama endpoint
        container += subtitle("Local AI Engine")
        val endpointEdit = editText(prefs.getString("ollama_endpoint", "192.168.1.100:11434")!!, "Ollama endpoint (IP:port)")
        container += endpointEdit
        val modelEdit = editText(prefs.getString("ollama_model", "qwen3:0.6b")!!, "Model name")
        container += modelEdit

        container += space()

        // Composio — ONE TIME connect, hidden after that
        container += subtitle("App Integrations")

        if (prefs.getBoolean(KEY_COMPOSIO_CONNECTED, false)) {
            container += checkRow("✅ Connected to Composio (1000+ apps)")
            container += body("Manage connections at app.composio.dev")

            // Hidden disconnect option
            container += button("Disconnect Composio") {
                prefs.edit()
                    .remove("composio_api_key")
                    .putBoolean(KEY_COMPOSIO_CONNECTED, false)
                    .apply()
                showSettings()
            }
        } else {
            container += body("Connect Composio to enable real actions:\n• Gmail, GitHub, Slack, Calendar, Notion, and 1000+ more\n• Skip to use INVOKE as voice dictation only")

            val keyEdit = editText("", "Paste Composio API Key (comp_...)")
            container += keyEdit

            container += button("🔗 Connect Composio") {
                val key = keyEdit.text.toString().trim()
                if (key.startsWith("comp_") || key.startsWith("sk-")) {
                    prefs.edit()
                        .putString("composio_api_key", key)
                        .putBoolean(KEY_COMPOSIO_CONNECTED, true)
                        .apply()
                    toast("✅ Composio connected!")
                    showSettings()
                } else {
                    toast("Invalid key — get one at app.composio.dev")
                }
            }
            container += button("Skip — dictation only") {
                prefs.edit().putBoolean(KEY_COMPOSIO_CONNECTED, false).apply()
                toast("Using dictation mode")
                showSettings()
            }
        }

        container += space()

        // Confidence threshold
        container += subtitle("Advanced")
        val confidenceLabel = textView("Confidence threshold: ${prefs.getInt("confidence_threshold", 60)}%")
        container += confidenceLabel
        val slider = SeekBar(this).apply {
            max = 100
            progress = prefs.getInt("confidence_threshold", 60)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    confidenceLabel.text = "Confidence threshold: $progress%"
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    prefs.edit().putInt("confidence_threshold", seekBar?.progress ?: 60).apply()
                }
            })
        }
        container += slider

        container += space()

        // Save Ollama settings
        container += button("💾 Save Settings") {
            prefs.edit()
                .putString("ollama_endpoint", endpointEdit.text.toString().trim())
                .putString("ollama_model", modelEdit.text.toString().trim())
                .apply()
            toast("Settings saved")
        }

        container += space()

        // Reset onboarding
        container += button("🔄 Redo Onboarding") {
            prefs.edit().putBoolean(KEY_ONBOARDING_DONE, false).apply()
            showOnboarding()
        }
    }

    // ─── Permissions ───

    private fun checkMicPermission(): Boolean =
        checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun requestMicPermission() {
        requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 100)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            prefs.edit().putBoolean(KEY_MIC_GRANTED, grantResults.isNotEmpty() && grantResults[0] == 0).apply()
            showOnboarding()
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabled = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabled.any { it.resolveInfo.serviceInfo.name.contains("invoke", true) }
    }

    // ─── UI helpers ───

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun title(text: String) = TextView(this).apply {
        this.text = text
        textSize = 24f
        setTextColor(0xFFFFFFFF.toInt())
        setPadding(0, dp(8), 0, dp(16))
    }

    private fun subtitle(text: String) = TextView(this).apply {
        this.text = text
        textSize = 18f
        setTextColor(0xFF8B5CF6.toInt())
        setPadding(0, dp(12), 0, dp(4))
    }

    private fun body(text: String) = TextView(this).apply {
        this.text = text
        textSize = 14f
        setTextColor(0xFFD1D5DB.toInt())
        setPadding(dp(4), dp(2), dp(4), dp(4))
    }

    private fun checkRow(text: String) = TextView(this).apply {
        this.text = text
        textSize = 15f
        setTextColor(0xFF22C55E.toInt())
        setPadding(0, dp(4), 0, dp(4))
    }

    private fun textView(text: String) = TextView(this).apply {
        this.text = text
        textSize = 14f
        setTextColor(0xFFE5E7EB.toInt())
        setPadding(0, dp(4), 0, dp(4))
    }

    private fun editText(hint: String, value: String = "") = EditText(this).apply {
        this.hint = hint
        setText(value)
        textSize = 14f
        setTextColor(0xFFFFFFFF.toInt())
        setHintTextColor(0xFF6B7280.toInt())
        inputType = InputType.TYPE_CLASS_TEXT
        setPadding(dp(12), dp(12), dp(12), dp(12))
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(0xFF1F2937.toInt())
            cornerRadius = 8 * resources.displayMetrics.density
        }
    }

    private fun button(text: String, onClick: () -> Unit) = com.google.android.material.button.MaterialButton(this).apply {
        this.text = text
        setTextColor(0xFFFFFFFF.toInt())
        backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF8B5CF6.toInt())
        setOnClickListener { onClick() }
        cornerRadius = 8 * resources.displayMetrics.density
    }

    private fun space() = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(12)) }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private operator fun LinearLayout.plusAssign(view: View) { addView(view) }
}
