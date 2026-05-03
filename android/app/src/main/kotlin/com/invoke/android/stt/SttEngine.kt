package com.invoke.android.stt

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.*
import java.io.File

/**
 * On-device speech-to-text engine for INVOKE.
 *
 * Uses sherpa-onnx to run local speech recognition with Moonshine, Whisper, or
 * NeMo Transducer models.  Models are discovered automatically from the
 * file layout under `context.filesDir/models/<name>/`.
 *
 * Typical usage (from a background thread):
 * ```
 * val engine = SttEngine.getInstance()
 * if (engine.init(context)) {
 *     val text = engine.transcribe(audioSamples)
 * }
 * ```
 */
class SttEngine private constructor() {

    // ---------------------------------------------------------------
    // Singleton
    // ---------------------------------------------------------------

    companion object {
        private const val TAG = "SttEngine"
        private const val MODELS_DIR = "models"

        @Volatile
        private var _instance: SttEngine? = null

        fun getInstance(): SttEngine =
            _instance ?: synchronized(SttEngine::class.java) {
                _instance ?: SttEngine().also { _instance = it }
            }
    }

    // ---------------------------------------------------------------
    // State
    // ---------------------------------------------------------------

    @Volatile
    private var recognizer: OfflineRecognizer? = null

    private val initLock = Any()

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Initialise the engine by loading the first valid model found under
     * `context.filesDir/models/`.
     *
     * Safe to call more than once; subsequent calls are no-ops once a model
     * is loaded.
     *
     * @return `true` if a model was loaded (or was already loaded).
     */
    fun init(context: Context): Boolean {
        if (recognizer != null) return true

        synchronized(initLock) {
            if (recognizer != null) return true

            val modelsRoot = File(context.filesDir, MODELS_DIR)
            if (!modelsRoot.isDirectory) {
                Log.e(TAG, "Models directory missing: ${modelsRoot.absolutePath}")
                return false
            }

            val candidates = modelsRoot.listFiles()
                ?.filter { it.isDirectory }
                ?: return false

            for (dir in candidates) {
                val config = buildConfigForDir(dir) ?: continue
                try {
                    recognizer = OfflineRecognizer(assetManager = null, config = config)
                    Log.i(TAG, "Loaded STT model from ${dir.name}")
                    return true
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load model in ${dir.name}", e)
                }
            }

            Log.e(TAG, "No loadable model found in ${modelsRoot.absolutePath}")
            return false
        }
    }

    /** Whether [init] has successfully loaded a model. */
    fun isReady(): Boolean = recognizer != null

    /**
     * Transcribe a mono PCM audio buffer.
     *
     * **Blocking** — call from a background thread.
     *
     * @param samples    float PCM samples, range [-1, 1].
     * @param sampleRate sample rate of the input audio (default 16 000 Hz).
     * @return recognised text, or an empty string on failure.
     */
    fun transcribe(samples: FloatArray, sampleRate: Int = 16000): String {
        val rec = recognizer ?: run {
            Log.w(TAG, "transcribe() called before init()")
            return ""
        }

        val stream = rec.createStream()
        try {
            stream.acceptWaveform(samples, sampleRate)
            rec.decode(stream)
            return rec.getResult(stream).text.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Transcription error", e)
            return ""
        } finally {
            stream.release()
        }
    }

    /**
     * List the names of model directories under `filesDir/models/`.
     * Each name can be used as a human-readable label; the engine picks
     * the first loadable one automatically during [init].
     */
    fun availableModels(context: Context): List<String> {
        val root = File(context.filesDir, MODELS_DIR)
        if (!root.isDirectory) return emptyList()
        return root.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?: emptyList()
    }

    // ---------------------------------------------------------------
    // Model-type detection
    // ---------------------------------------------------------------

    private sealed class DetectedType {
        data class Moonshine(val dir: File) : DetectedType()
        data class Whisper(val dir: File) : DetectedType()
        data class Transducer(val dir: File) : DetectedType()
    }

    /**
     * Detect the model architecture from the files present in [dir]:
     *
     *  1. **Moonshine** — `preprocess.onnx` exists.
     *  2. **NeMo Transducer** — encoder + decoder + joiner all present.
     *  3. **Whisper** — encoder + decoder present, no joiner.
     */
    private fun detectType(dir: File): DetectedType? {
        val names = dir.listFiles()?.map { it.name.lowercase() } ?: return null

        // 1. Moonshine v1
        if (names.any { it == "preprocess.onnx" }) {
            return DetectedType.Moonshine(dir)
        }

        val hasEncoder = names.any { isEncoder(it) }
        val hasDecoder = names.any { isDecoder(it) }
        val hasJoiner  = names.any { isJoiner(it) }

        // 2. NeMo Transducer
        if (hasEncoder && hasDecoder && hasJoiner) {
            return DetectedType.Transducer(dir)
        }

        // 3. Whisper
        if (hasEncoder && hasDecoder) {
            return DetectedType.Whisper(dir)
        }

        return null
    }

    // ---------------------------------------------------------------
    // File-matching helpers
    // ---------------------------------------------------------------

    private fun isEncoder(name: String): Boolean {
        val n = name.lowercase()
        return isValidModelFile(n)
                && (n.contains("encoder") || (n.startsWith("encode") && !n.contains("decode")))
    }

    private fun isDecoder(name: String): Boolean {
        val n = name.lowercase()
        return isValidModelFile(n) && n.contains("decoder")
    }

    private fun isJoiner(name: String): Boolean {
        val n = name.lowercase()
        return isValidModelFile(n) && n.contains("joiner")
    }

    private fun isValidModelFile(name: String): Boolean =
        name.endsWith(".onnx") || name.endsWith(".ort")

    /**
     * Find the best-matching model file in [dir] using [matcher].
     * When multiple candidates exist the one containing "int8" is preferred.
     */
    private fun pickFile(dir: File, matcher: (String) -> Boolean): File? {
        val hits = dir.listFiles()?.filter { matcher(it.name) } ?: return null
        if (hits.isEmpty()) return null
        return hits.find { it.name.contains("int8", ignoreCase = true) }
            ?: hits.first()
    }

    private fun findTokens(dir: File): File? {
        File(dir, "tokens.txt").let { if (it.exists()) return it }
        return dir.listFiles()?.find { it.name.lowercase().contains("tokens") }
    }

    // ---------------------------------------------------------------
    // Config builders
    // ---------------------------------------------------------------

    private fun buildConfigForDir(dir: File): OfflineRecognizerConfig? {
        val type = detectType(dir) ?: return null
        val tokens = findTokens(dir) ?: return null

        val modelCfg: OfflineModelConfig = when (type) {
            is DetectedType.Moonshine   -> moonshineConfig(type.dir, tokens)
            is DetectedType.Whisper     -> whisperConfig(type.dir, tokens)
            is DetectedType.Transducer  -> transducerConfig(type.dir, tokens)
        } ?: return null

        return OfflineRecognizerConfig(modelConfig = modelCfg)
    }

    /**
     * Moonshine v1: `preprocess.onnx` + encoder + optional
     * `uncached_decode` / `cached_decode` / `merged` decoder files.
     */
    private fun moonshineConfig(dir: File, tokens: File): OfflineModelConfig? {
        val preprocessor = File(dir, "preprocess.onnx")
        if (!preprocessor.exists()) return null

        val encoder = pickFile(dir) { n ->
            val l = n.lowercase()
            isValidModelFile(l)
                    && (l.contains("encode"))
                    && !l.contains("decode")
                    && !l.contains("preprocess")
        } ?: return null

        val uncachedDec = pickFile(dir) { n ->
            val l = n.lowercase()
            isValidModelFile(l) && l.contains("uncached")
        }

        val cachedDec = pickFile(dir) { n ->
            val l = n.lowercase()
            isValidModelFile(l) && l.contains("cached") && !l.contains("uncached")
        }

        val mergedDec = pickFile(dir) { n ->
            val l = n.lowercase()
            isValidModelFile(l) && l.contains("merged")
        }

        return OfflineModelConfig(
            moonshine = OfflineMoonshineModelConfig(
                preprocessor = preprocessor.absolutePath,
                encoder = encoder.absolutePath,
                uncachedDecoder = uncachedDec?.absolutePath ?: "",
                cachedDecoder = cachedDec?.absolutePath ?: "",
                mergedDecoder = mergedDec?.absolutePath ?: "",
            ),
            tokens = tokens.absolutePath,
            numThreads = 2,
            debug = false,
            provider = "cpu",
        )
    }

    /** Whisper: encoder + decoder, no joiner. */
    private fun whisperConfig(dir: File, tokens: File): OfflineModelConfig? {
        val encoder = pickFile(dir, ::isEncoder) ?: return null
        val decoder = pickFile(dir, ::isDecoder) ?: return null

        return OfflineModelConfig(
            whisper = OfflineWhisperModelConfig(
                encoder = encoder.absolutePath,
                decoder = decoder.absolutePath,
            ),
            tokens = tokens.absolutePath,
            modelType = "whisper",
            numThreads = 2,
            debug = false,
            provider = "cpu",
        )
    }

    /** NeMo Transducer: encoder + decoder + joiner. */
    private fun transducerConfig(dir: File, tokens: File): OfflineModelConfig? {
        val encoder = pickFile(dir, ::isEncoder) ?: return null
        val decoder = pickFile(dir, ::isDecoder) ?: return null
        val joiner  = pickFile(dir, ::isJoiner)  ?: return null

        return OfflineModelConfig(
            transducer = OfflineTransducerModelConfig(
                encoder = encoder.absolutePath,
                decoder = decoder.absolutePath,
                joiner = joiner.absolutePath,
            ),
            tokens = tokens.absolutePath,
            modelType = "transducer",
            numThreads = 2,
            debug = false,
            provider = "cpu",
        )
    }
}
