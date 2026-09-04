package com.mogralabs.mogra.audio

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp
import kotlin.math.max

/**
 * The whole identifier: Sa-anchored CQT network and pitch histogram, calibrated, averaged,
 * top five out. A transcription of `raag_fusion.identifier`.
 *
 * The two branches produce scores on completely different scales, so each is turned into a
 * distribution by a softmax whose temperature was fitted on validation, and only then are
 * they mixed — 0.60 on the network, 0.40 on the histogram. Both numbers come from
 * `config.json` rather than being written down here.
 *
 * **The tonic is required.** A raag is a pattern of intervals above Sa, not a set of
 * frequencies, so a model given the wrong Sa is not slightly wrong — it is answering a
 * different question. The survey behind these weights measured it: the same network scored
 * 0.302 with Sa anchored and 0.087 with the tonics permuted, against 0.020 for guessing.
 */
class RaagIdentifier private constructor(
    private val cqtNet: Module,
    private val crepe: Module,
    private val linear: Melody.LinearModel,
    private val binCents: FloatArray,
    val raags: List<String>,
    private val melodyWeight: Double,
    private val temperatureCqt: Double,
    private val temperatureMelody: Double,
) {

    data class Prediction(val raag: String, val probability: Double)

    /**
     * Fused probabilities for one recording, averaged over its 20 s windows.
     *
     * [kernel] is optional only so a caller that already built it for this tonic can pass
     * it back in; building it is the one costly thing that does not depend on the audio.
     */
    fun probabilities(
        y: FloatArray, sr: Int, tonicHz: Double,
        kernel: Cqt.Kernel = Cqt.Kernel.forTonic(tonicHz),
        onProgress: ((Int, Int) -> Unit)? = null,
    ): DoubleArray {
        require(tonicHz.isFinite() && tonicHz > 0) { "tonicHz is required" }
        val windows = Resampler.windows(y, sr)
        val summed = DoubleArray(raags.size)
        windows.forEachIndexed { i, w ->
            val p = windowProbabilities(w, sr, tonicHz, kernel)
            for (j in summed.indices) summed[j] += p[j]
            onProgress?.invoke(i + 1, windows.size)
        }
        for (j in summed.indices) summed[j] /= windows.size
        return summed
    }

    fun predict(
        y: FloatArray, sr: Int, tonicHz: Double, topK: Int = 5,
        kernel: Cqt.Kernel = Cqt.Kernel.forTonic(tonicHz),
        onProgress: ((Int, Int) -> Unit)? = null,
    ): List<Prediction> {
        val p = probabilities(y, sr, tonicHz, kernel, onProgress)
        return p.indices.sortedByDescending { p[it] }.take(topK)
            .map { Prediction(raags[it], p[it]) }
    }

    private fun windowProbabilities(
        w: FloatArray, sr: Int, tonicHz: Double, kernel: Cqt.Kernel,
    ): DoubleArray {
        // branch 1 -- 22.05 kHz, peak-normalised, exactly 20 s
        val y22 = Resampler.fitLength(
            Resampler.peakNormalise(Resampler.resample(w, sr, Cqt.SR)),
            Math.round(Cqt.SR * Cqt.WINDOW_SECONDS).toInt(),
        )
        val x = Cqt.features(y22, kernel)
        val input = Tensor.fromBlob(x, longArrayOf(1, 1, Cqt.N_BINS.toLong(), Cqt.N_FRAMES.toLong()))
        val logits = cqtNet.forward(IValue.from(input)).toTensor().dataAsFloatArray
        val pCqt = softmax(DoubleArray(logits.size) { logits[it].toDouble() }, temperatureCqt)

        // branch 2 -- 16 kHz, deliberately *not* normalised; CREPE saw the raw decode
        val y16 = Resampler.resample(w, sr, Melody.SR)
        val frames = Melody.frames(y16)
        val flat = FloatArray(frames.size * Melody.WINDOW)
        frames.forEachIndexed { i, f -> System.arraycopy(f, 0, flat, i * Melody.WINDOW, f.size) }
        val fTensor = Tensor.fromBlob(flat, longArrayOf(frames.size.toLong(), Melody.WINDOW.toLong()))
        val out = crepe.forward(IValue.from(fTensor)).toTensor().dataAsFloatArray
        val probs = Array(frames.size) { t ->
            FloatArray(Melody.PITCH_BINS) { b -> out[t * Melody.PITCH_BINS + b] }
        }
        val (f0, voiced) = Melody.decode(probs, binCents)
        val hist = Melody.histogram(f0, voiced, tonicHz)
        val pMel = softmax(linear.scores(hist), temperatureMelody)

        return DoubleArray(pCqt.size) { (1.0 - melodyWeight) * pCqt[it] + melodyWeight * pMel[it] }
    }

    private fun softmax(x: DoubleArray, temperature: Double): DoubleArray {
        val t = max(temperature, 1e-6)
        var top = Double.NEGATIVE_INFINITY
        for (v in x) top = max(top, v / t)
        var sum = 0.0
        val out = DoubleArray(x.size) { exp(x[it] / t - top).also { e -> sum += e } }
        for (i in out.indices) out[i] /= sum
        return out
    }

    companion object {
        private const val DIR = "model"

        fun load(context: Context): RaagIdentifier {
            val assets = context.assets
            val config = JSONObject(assets.open("$DIR/config.json").bufferedReader().readText())
            val raagsJson = JSONArray(assets.open("$DIR/raags.json").bufferedReader().readText())
            val raags = (0 until raagsJson.length()).map { raagsJson.getString(it) }

            return RaagIdentifier(
                cqtNet = LiteModuleLoader.load(unpack(context, "cqt_net.ptl")),
                crepe = LiteModuleLoader.load(unpack(context, "crepe_tiny.ptl")),
                linear = readLinear(assets.open("$DIR/melody_linear.bin").readBytes()),
                binCents = readBinCents(assets.open("$DIR/bin_cents.bin").readBytes()),
                raags = raags,
                melodyWeight = config.getDouble("melody_weight"),
                temperatureCqt = config.getDouble("temperature_cqt"),
                temperatureMelody = config.getDouble("temperature_melody"),
            )
        }

        /**
         * Copy a checkpoint out of the APK and return its path, copying only once.
         *
         * The nets are stored compressed — uncompressed they push the APK over 30 MB — and
         * PyTorch wants a real file rather than a compressed asset stream, so they are
         * unpacked into app storage the first time the model is loaded.
         */
        private fun unpack(context: Context, name: String): String {
            val out = java.io.File(context.filesDir, name)
            // openFd() is not an option: it throws on a compressed asset, which is exactly
            // what these are. Re-copy whenever the installed package is newer than the copy.
            val installed = runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
            }.getOrDefault(0L)
            if (!out.exists() || out.length() == 0L || out.lastModified() < installed) {
                context.assets.open("$DIR/$name").use { input ->
                    out.outputStream().use { input.copyTo(it) }
                }
            }
            return out.absolutePath
        }

        /** "MGML", int32 nBins, int32 nRaags, then mean, scale, coef row-major, intercept. */
        fun readLinear(bytes: ByteArray): Melody.LinearModel {
            val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val magic = ByteArray(4).also { bb.get(it) }
            require(String(magic) == "MGML") { "melody_linear.bin: bad magic" }
            val nBins = bb.int
            val nRaags = bb.int
            val mean = FloatArray(nBins) { bb.float }
            val scale = FloatArray(nBins) { bb.float }
            val coef = Array(nRaags) { FloatArray(nBins) { bb.float } }
            val intercept = FloatArray(nRaags) { bb.float }
            return Melody.LinearModel(mean, scale, coef, intercept)
        }

        /** "MGBC", int32 n, then n float32 — the dithered CREPE bin centres. */
        fun readBinCents(bytes: ByteArray): FloatArray {
            val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val magic = ByteArray(4).also { bb.get(it) }
            require(String(magic) == "MGBC") { "bin_cents.bin: bad magic" }
            return FloatArray(bb.int) { bb.float }
        }
    }
}
