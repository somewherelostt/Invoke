package com.invoke.android.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
    }

    private enum class PipelineState { IDLE, RECORDING, TRANSCRIBING, CLASSIFYING, EXECUTING }

    private var pipelineState = PipelineState.IDLE
    private var bubble: InvokeBubble? = null
    private var audioRecord: AudioRecord? = null
    private var pcmStream: ByteArrayOutputStream? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val agentClient = AgentClient()

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
            if (!SttEngine.isReady()) {
                val ok = SttEngine.init(this@InvokeAccessibilityService)
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

    // ─── Recording ───

    private fun startRecording() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            toast("Grant microphone permission in INVOKE app")
            return
        }

        val bufSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        audioRecord = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize
            )
        } catch (_: SecurityException) {
            toast("Microphone permission denied"); return
        }

        pcmStream = ByteArrayOutputStream()
        audioRecord!!.startRecording()
        pipelineState = PipelineState.RECORDING
        bubble?.setState(InvokeBubble.State.RECORDING)

        // Read PCM on background thread
        scope.launch(Dispatchers.IO) {
            val buf = ByteArray(bufSize)
            while (pipelineState == PipelineState.RECORDING) {
                val n = audioRecord?.read(buf, 0, buf.size) ?: break
                if (n > 0) pcmStream?.write(buf, 0, n)
            }
        }
    }

    private fun stopAndProcess() {
        pipelineState = PipelineState.TRANSCRIBING
        bubble?.setState(InvokeBubble.State.PROCESSING)

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        val pcm = pcmStream?.toByteArray() ?: ByteArray(0)
        pcmStream = null

        if (pcm.isEmpty()) {
            bubble?.showFeedback("No audio captured")
            resetToIdle(); return
        }

        scope.launch {
            try {
                // Step 1: Transcribe (Whisper / sherpa-onnx)
                val samples = pcmToFloats(pcm)
                val transcription = withContext(Dispatchers.IO) {
                    SttEngine.transcribe(samples, SAMPLE_RATE)
                }

                Log.i(TAG, "Transcription: \"$transcription\"")

                if (transcription.isBlank()) {
                    bubble?.showFeedback("No speech detected")
                    resetToIdle(); return@launch
                }

                bubble?.showFeedback(transcription, 3000)

                // Step 2: Classify + Execute (Qwen → Composio)
                pipelineState = PipelineState.CLASSIFYING
                val prefs = getSharedPreferences("invoke_prefs", MODE_PRIVATE)
                val result = agentClient.classifyAndExecute(transcription, prefs)

                Log.i(TAG, "Action result: success=${result.success} tool=${result.tool}")

                // Step 3: Inject result or show feedback
                if (result.isDictation && result.text.isNotBlank()) {
                    injectText(result.text)
                    bubble?.showFeedback("✓ Inserted", 1500)
                } else if (result.success) {
                    bubble?.showFeedback("✓ ${result.tool}: Done", 2500)
                } else {
                    bubble?.showFeedback("✗ ${result.text.take(50)}", 3000)
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

    // ─── Text Injection (works in ANY app) ───

    private fun injectText(text: String) {
        // Always copy to clipboard as fallback
        val clip = ClipData.newPlainText("invoke", text)
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)

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
            SttEngine.init(this@InvokeAccessibilityService)
        }
    }
}
