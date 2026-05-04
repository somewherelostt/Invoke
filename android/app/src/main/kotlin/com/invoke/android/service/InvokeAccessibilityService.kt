package com.invoke.android.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.invoke.android.agent.AgentClient
import com.invoke.android.overlay.InvokeBubble
import com.invoke.android.stt.SttEngine
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream

/**
 * Core INVOKE accessibility service.
 * Floating bubble → record → transcribe (Whisper) → classify (Qwen) → execute (Composio) → inject text.
 */
class InvokeAccessibilityService : AccessibilityService() {

    companion object {
        var instance: InvokeAccessibilityService? = null
        private set
        private const val TAG = "INVOKE"
        private const val SAMPLE_RATE = 16000
        private const val MIN_RECORDING_MS = 650L
        private const val MIN_PCM_BYTES = SAMPLE_RATE * 2 / 2
        private const val MIN_RMS = 0.0035f
        private const val MIN_PEAK = 0.025f
    }

    private enum class PipelineState { IDLE, RECORDING, TRANSCRIBING, CLASSIFYING, EXECUTING }

    private var pipelineState = PipelineState.IDLE
    private var bubble: InvokeBubble? = null
    private var audioRecord: AudioRecord? = null
    private var pcmStream: ByteArrayOutputStream? = null
    private var recordingStartedAtMs = 0L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val agentClient = AgentClient()
    private val sttEngine = SttEngine.getInstance()

    // ─── Lifecycle ───

    override fun onServiceConnected() {
        instance = this
        Log.i(TAG, "Accessibility service connected")

        val wm = getSystemService(WINDOW_SERVICE) as android.view.WindowManager
        bubble = InvokeBubble(this).also { b ->
            b.attach(wm)
            b.setOnTapListener { onBubbleTap() }
        }

        // Load STT model in background
        scope.launch(Dispatchers.IO) {
            if (!sttEngine.isReady()) {
                val ok = sttEngine.init(this@InvokeAccessibilityService)
                Log.i(TAG, "STT engine init: $ok")
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We don't need to react to events for always-visible bubble mode
        // But we listen so we have window access for text injection
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        scope.cancel()
        bubble?.detach()
        super.onDestroy()
    }

    // ─── Bubble Tap → Pipeline ───

    private fun onBubbleTap() {
        when (pipelineState) {
            PipelineState.IDLE -> startRecording()
            PipelineState.RECORDING -> stopAndProcess()
            else -> { /* busy, ignore */ }
        }
    }

    fun triggerVoiceAction() {
        onBubbleTap()
    }

    // ─── Recording ───

    private fun startRecording() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            toast("Grant microphone permission in INVOKE app")
            return
        }

        val minBufSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufSize <= 0) {
            Log.e(TAG, "Invalid AudioRecord min buffer size: $minBufSize")
            toast("Could not start microphone")
            return
        }
        val bufSize = maxOf(minBufSize, SAMPLE_RATE / 2)
        audioRecord = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize
            )
        } catch (_: SecurityException) {
            toast("Microphone permission denied"); return
        }

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord not initialized")
            audioRecord?.release()
            audioRecord = null
            toast("Microphone is not available")
            return
        }

        pcmStream = ByteArrayOutputStream()
        try {
            audioRecord!!.startRecording()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "startRecording failed", e)
            audioRecord?.release()
            audioRecord = null
            toast("Could not start recording")
            return
        }
        if (audioRecord?.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            Log.e(TAG, "AudioRecord did not enter recording state")
            audioRecord?.release()
            audioRecord = null
            toast("Microphone did not start")
            return
        }

        recordingStartedAtMs = android.os.SystemClock.elapsedRealtime()
        pipelineState = PipelineState.RECORDING
        bubble?.setState(InvokeBubble.State.RECORDING)
        bubble?.showFeedback("Listening. Tap again to stop.", 1800)

        // Read PCM on background thread
        scope.launch(Dispatchers.IO) {
            val buf = ByteArray(bufSize)
            while (pipelineState == PipelineState.RECORDING) {
                val n = audioRecord?.read(buf, 0, buf.size) ?: break
                if (n > 0) {
                    pcmStream?.write(buf, 0, n)
                } else if (n < 0) {
                    Log.w(TAG, "AudioRecord read returned $n")
                }
            }
        }
    }

    private fun stopAndProcess() {
        pipelineState = PipelineState.TRANSCRIBING
        bubble?.setState(InvokeBubble.State.PROCESSING)

        try {
            audioRecord?.stop()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "AudioRecord stop failed", e)
        }
        audioRecord?.release()
        audioRecord = null

        val pcm = pcmStream?.toByteArray() ?: ByteArray(0)
        pcmStream = null
        val elapsedMs = android.os.SystemClock.elapsedRealtime() - recordingStartedAtMs

        if (pcm.isEmpty() || pcm.size < MIN_PCM_BYTES || elapsedMs < MIN_RECORDING_MS) {
            Log.i(TAG, "Recording too short/empty: bytes=${pcm.size}, elapsedMs=$elapsedMs")
            bubble?.showFeedback("No audio captured")
            resetToIdle(); return
        }

        scope.launch {
            try {
                // Step 1: Transcribe (Whisper / sherpa-onnx)
                val samples = pcmToFloats(pcm)
                val level = audioLevel(samples)
                Log.i(TAG, "Audio level: rms=${level.rms}, peak=${level.peak}, samples=${samples.size}")
                if (level.rms < MIN_RMS && level.peak < MIN_PEAK) {
                    bubble?.showFeedback("No speech detected")
                    resetToIdle(); return@launch
                }

                val transcription = withContext(Dispatchers.IO) {
                    sttEngine.transcribe(samples, SAMPLE_RATE)
                }.cleanTranscript()

                Log.i(TAG, "Transcription: \"$transcription\"")

                if (transcription.isBlank() || isNoSpeechText(transcription)) {
                    bubble?.showFeedback("No speech detected")
                    resetToIdle(); return@launch
                }

                bubble?.showFeedback(transcription, 3000)

                val prefs = getSharedPreferences("invoke_prefs", MODE_PRIVATE)
                if (shouldFastPaste(transcription, prefs)) {
                    injectText(transcription)
                    bubble?.showFeedback("Inserted", 1200)
                    bubble?.setState(InvokeBubble.State.DONE)
                    resetToIdle()
                    return@launch
                }

                // Step 2: Classify + Execute (Qwen -> Composio)
                pipelineState = PipelineState.CLASSIFYING
                val result = agentClient.classifyAndExecute(transcription, prefs)

                Log.i(TAG, "Action result: success=${result.success} tool=${result.tool}")

                // Step 3: Inject result or show feedback
                if (result.isDictation && result.text.isNotBlank() && !isNoSpeechText(result.text)) {
                    injectText(result.text)
                    bubble?.showFeedback("Inserted", 1500)
                } else if (result.success) {
                    bubble?.showFeedback("${result.tool}: Done", 2500)
                } else {
                    bubble?.showFeedback(result.text.take(50), 3000)
                }

                bubble?.setState(InvokeBubble.State.DONE)
                resetToIdle()
            } catch (e: Exception) {
                Log.e(TAG, "Pipeline error", e)
                bubble?.showFeedback("Error: ${e.message?.take(40)}")
                resetToIdle()
            }
        }
    }

    private fun resetToIdle() {
        pipelineState = PipelineState.IDLE
        bubble?.setState(InvokeBubble.State.IDLE)
    }

    // ─── PCM → Float conversion ───

    private fun pcmToFloats(pcm: ByteArray): FloatArray {
        val samples = FloatArray(pcm.size / 2)
        for (i in samples.indices) {
            val lo = pcm[i * 2].toInt() and 0xFF
            val hi = pcm[i * 2 + 1].toInt()
            samples[i] = ((hi shl 8) or lo).toShort().toFloat() / 32768f
        }
        return samples
    }

    private data class AudioLevel(val rms: Float, val peak: Float)

    private fun audioLevel(samples: FloatArray): AudioLevel {
        if (samples.isEmpty()) return AudioLevel(0f, 0f)
        var sumSquares = 0.0
        var peak = 0f
        for (sample in samples) {
            val abs = kotlin.math.abs(sample)
            if (abs > peak) peak = abs
            sumSquares += (sample * sample).toDouble()
        }
        val rms = kotlin.math.sqrt(sumSquares / samples.size).toFloat()
        return AudioLevel(rms, peak)
    }

    private fun String.cleanTranscript(): String =
        trim()
            .trim('"', '\'', '.', ',', ' ', '\n', '\r', '\t')
            .replace(Regex("\\s+"), " ")

    private fun isNoSpeechText(text: String): Boolean {
        val normalized = text
            .lowercase()
            .trim()
            .trim('.', ',', '!', '?', '"', '\'', '(', ')', '[', ']', '{', '}')
        return normalized.isBlank() || normalized in setOf(
            "silence",
            "silent",
            "no speech",
            "no audio",
            "blank audio",
            "inaudible",
            "background noise",
            "noise",
            "music",
            "uh",
            "um",
            "hmm",
            "..."
        ) || normalized.contains("<|nospeech|>") ||
            normalized.contains("blank_audio") ||
            normalized.contains("no_speech")
    }

    private fun shouldFastPaste(text: String, prefs: SharedPreferences): Boolean {
        val composioKey = prefs.getString("composio_api_key", "").orEmpty()
        return composioKey.isBlank() || !looksLikeActionCommand(text)
    }

    private fun looksLikeActionCommand(text: String): Boolean {
        val normalized = text.lowercase().trim()
        val commandPrefixes = listOf(
            "send ",
            "email ",
            "mail ",
            "create ",
            "open ",
            "search ",
            "look up ",
            "google ",
            "find ",
            "schedule ",
            "add ",
            "post ",
            "message ",
            "slack ",
            "github ",
            "notion ",
            "calendar "
        )
        val commandPhrases = listOf(
            "send an email",
            "send email",
            "create an issue",
            "create issue",
            "search the web",
            "web search",
            "list my emails",
            "read my emails"
        )
        return commandPrefixes.any { normalized.startsWith(it) } ||
            commandPhrases.any { normalized.contains(it) }
    }

    // Text injection (works in most editable apps)

    private fun injectText(text: String) {
        // Always copy to clipboard as fallback
        val clip = ClipData.newPlainText("invoke", text)
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)

        if (tryFocusedInjection(text)) {
            return
        }

        // Find best injection target
        val candidates = findInjectionCandidates()
        Log.i(TAG, "Found ${candidates.size} injection candidates")

        var injected = false
        try {
            for (node in candidates) {
                if (tryInject(node, text)) {
                    injected = true
                    break
                }
            }
        } finally {
            candidates.forEach { it.recycle() }
        }

        if (!injected) {
            Log.i(TAG, "No direct injection succeeded — clipboard fallback")
        }
    }

    private fun tryFocusedInjection(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        return try {
            val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                ?: root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
            if (focused == null) {
                false
            } else {
                try {
                    tryInject(focused, text)
                } finally {
                    focused.recycle()
                }
            }
        } finally {
            root.recycle()
        }
    }

    private fun findInjectionCandidates(): List<AccessibilityNodeInfo> {
        val candidates = mutableListOf<AccessibilityNodeInfo>()

        // Search active window
        rootInActiveWindow?.let { root ->
            collectTargets(root, candidates)
            root.recycle()
        }

        // Search all active/focused windows
        windows?.filter { it.isActive || it.isFocused }?.forEach { window ->
            val root = window.root ?: return@forEach
            collectTargets(root, candidates)
            root.recycle()
        }

        return candidates.sortedByDescending(::scoreNode)
    }

    private fun collectTargets(node: AccessibilityNodeInfo, out: MutableList<AccessibilityNodeInfo>) {
        // Find focused input
        node.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.let { out += it }
        // Collect editable/focused nodes
        scanForTargets(node, out)
    }

    private fun scanForTargets(node: AccessibilityNodeInfo, out: MutableList<AccessibilityNodeInfo>) {
        if (isTarget(node)) {
            out += AccessibilityNodeInfo.obtain(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try { scanForTargets(child, out) } finally { child.recycle() }
        }
    }

    private fun isTarget(node: AccessibilityNodeInfo): Boolean {
        val cls = node.className?.toString().orEmpty()
        return node.isFocused || node.isEditable ||
            cls.contains("EditText") || cls.contains("TerminalView") ||
            hasCustomPaste(node)
    }

    private fun scoreNode(node: AccessibilityNodeInfo): Int {
        val cls = node.className?.toString().orEmpty()
        var score = 0
        if (hasCustomPaste(node)) score += 100
        if (cls.contains("TerminalView")) score += 80
        if (node.isEditable) score += 60
        if (node.isFocused) score += 40
        if (cls.contains("EditText")) score += 20
        return score
    }

    private fun tryInject(node: AccessibilityNodeInfo, text: String): Boolean {
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)

        // Strategy 1: Custom paste action
        findCustomPaste(node)?.let { action ->
            if (node.performAction(action.id)) return true
        }

        // Strategy 2: Standard paste
        if (node.performAction(AccessibilityNodeInfo.ACTION_PASTE)) return true

        // Strategy 3: Set text at cursor position
        if (node.isEditable || node.className?.toString()?.contains("EditText") == true) {
            val current = node.text?.toString().orEmpty()
            val start = if (node.textSelectionStart >= 0) node.textSelectionStart else current.length
            val end = if (node.textSelectionEnd >= 0) node.textSelectionEnd else start
            val updated = current.replaceRange(minOf(start, end), maxOf(start, end), text)
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, updated)
            }
            if (node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) return true
        }

        return false
    }

    private fun hasCustomPaste(node: AccessibilityNodeInfo): Boolean =
        node.actionList.any { it.label?.toString()?.contains("paste", true) == true }

    private fun findCustomPaste(node: AccessibilityNodeInfo): AccessibilityNodeInfo.AccessibilityAction? =
        node.actionList.firstOrNull { it.label?.toString()?.contains("paste", true) == true }

    private fun toast(msg: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    /** Reload STT model (called from MainActivity after model download) */
    fun reloadSttModel() {
        scope.launch(Dispatchers.IO) {
            sttEngine.init(this@InvokeAccessibilityService)
        }
    }
}
