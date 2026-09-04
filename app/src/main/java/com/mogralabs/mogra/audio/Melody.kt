package com.mogralabs.mogra.audio

import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * The pitch-histogram branch — a Kotlin transcription of `model/portable/melody.py`.
 *
 * CREPE gives a frame-level f0 track; every voiced frame is expressed in cents above Sa,
 * folded into one octave and dropped into 120 bins, blurred slightly so two performances
 * tuned a few cents apart still overlap, and raised to the power 0.5 so one long held nyas
 * note cannot swamp every other swar.
 *
 * Two inherited quirks that are invisible if you get them wrong:
 *
 * * the exported net already ends in a sigmoid, and the decode applies a **second** one
 *   before weighting — the histograms the linear model was fitted on came through both;
 * * the 360 bin centres are **dithered**, by a fixed draw that lives in `bin_cents.bin`.
 *   It is a constant, not something to regenerate.
 *
 * The hop is 40 ms rather than the 10 ms the track was originally computed at. Frames at
 * hop 2h are exactly the even-indexed frames at hop h, so a coarser hop is a uniform
 * subsample of the same track. Measured over the 150-clip test set the fused scores are
 * 10 ms → 0.480/0.820, 20 ms → 0.480/0.820, 40 ms → 0.487/0.820, 80 ms → 0.467/0.807. At
 * 40 ms it is a quarter of the work for no loss; 80 ms is where the histogram stops
 * separating a swar from the meend leading into it.
 */
object Melody {

    const val SR = 16000
    const val HOP = 640                 // 40 ms
    const val HOP_TRAINED = 160         // 10 ms, what the histograms were computed at
    const val WINDOW = 1024
    const val PITCH_BINS = 360
    const val CONFIDENCE = 0.4f
    const val N_BINS = 120
    private const val SMOOTH = 1.0
    private const val POWER = 0.5
    private const val CENTS_PER_BIN = 20.0
    private const val CENTS_0 = 1997.3794084376191
    private const val FMIN = 50.0
    private const val FMAX = 2000.0

    /** Frames of [WINDOW] samples every [hop], mean-centred and scaled by their own s.d. */
    fun frames(y16000: FloatArray, hop: Int = HOP): Array<FloatArray> {
        val total = 1 + y16000.size / hop
        val padded = FloatArray(y16000.size + WINDOW)
        System.arraycopy(y16000, 0, padded, WINDOW / 2, y16000.size)
        return Array(total) { i ->
            val f = FloatArray(WINDOW)
            System.arraycopy(padded, i * hop, f, 0, WINDOW)
            var mean = 0.0
            for (v in f) mean += v
            mean /= WINDOW
            var ss = 0.0
            for (j in 0 until WINDOW) {
                f[j] = (f[j] - mean).toFloat()
                ss += f[j].toDouble() * f[j]
            }
            val sd = max(1e-10, sqrt(ss / (WINDOW - 1)))     // unbiased, as torch.std is
            for (j in 0 until WINDOW) f[j] = (f[j] / sd).toFloat()
            f
        }
    }

    private fun frequencyToBin(hz: Double, ceil: Boolean): Int {
        val cents = 1200.0 * (ln(hz / 10.0) / ln(2.0))
        val b = (cents - CENTS_0) / CENTS_PER_BIN
        return if (ceil) Math.ceil(b).toInt() else floor(b).toInt()
    }

    /**
     * Network output → (f0 in Hz, voiced mask).
     *
     * [probs] is `nFrames × 360`, already sigmoid, exactly as the exported module returns.
     */
    fun decode(probs: Array<FloatArray>, binCents: FloatArray): Pair<FloatArray, BooleanArray> {
        val lo = frequencyToBin(FMIN, ceil = false)
        val hi = frequencyToBin(FMAX, ceil = true)
        val f0 = FloatArray(probs.size)
        val voiced = BooleanArray(probs.size)

        for (t in probs.indices) {
            val p = probs[t]
            var best = lo
            for (b in lo until hi) if (p[b] > p[best]) best = b
            voiced[t] = p[best] >= CONFIDENCE

            val from = max(0, best - 4)
            val to = minOf(PITCH_BINS, best + 5)
            var num = 0.0
            var den = 0.0
            for (b in from until to) {
                if (b < lo || b >= hi) continue          // masked out before the decode
                val s = 1.0 / (1.0 + exp(-p[b].toDouble()))   // the second sigmoid
                num += s * binCents[b]
                den += s
            }
            val cents = if (den > 0) num / den else binCents[best].toDouble()
            f0[t] = (10.0 * 2.0.pow(cents / 1200.0)).toFloat()
        }
        return f0 to voiced
    }

    /** Voiced frames → a 120-bin octave-folded pitch histogram summing to 1. */
    fun histogram(f0: FloatArray, voiced: BooleanArray, tonicHz: Double): FloatArray {
        val h = DoubleArray(N_BINS)
        var n = 0
        for (i in f0.indices) {
            if (!voiced[i]) continue
            val f = f0[i].toDouble()
            if (f <= 0 || !f.isFinite()) continue
            val cents = 1200.0 * (ln(f / tonicHz) / ln(2.0))
            if (!cents.isFinite()) continue
            var folded = cents % 1200.0
            if (folded < 0) folded += 1200.0
            var idx = floor(folded * (N_BINS / 1200.0)).toInt() % N_BINS
            if (idx < 0) idx += N_BINS
            h[idx] += 1.0
            n++
        }
        if (n < 5) return FloatArray(N_BINS)

        // circular Gaussian blur, done directly -- 120 bins does not need an FFT
        val kern = DoubleArray(N_BINS) { val d = minOf(it, N_BINS - it); exp(-0.5 * (d / SMOOTH).pow(2)) }
        var ks = 0.0
        for (v in kern) ks += v
        val blurred = DoubleArray(N_BINS)
        for (i in 0 until N_BINS) {
            var acc = 0.0
            for (j in 0 until N_BINS) acc += h[j] * kern[((i - j) % N_BINS + N_BINS) % N_BINS]
            blurred[i] = max(0.0, acc / ks)
        }
        var total = 0.0
        for (i in 0 until N_BINS) { blurred[i] = blurred[i].pow(POWER); total += blurred[i] }
        return FloatArray(N_BINS) { if (total > 0) (blurred[it] / total).toFloat() else 0f }
    }

    /**
     * Standardise, then multinomial logistic regression — four arrays, not a pickled
     * scikit-learn object, so nothing here is tied to a library version.
     */
    class LinearModel(
        val mean: FloatArray, val scale: FloatArray,
        val coef: Array<FloatArray>, val intercept: FloatArray,
    ) {
        fun scores(hist: FloatArray): DoubleArray = DoubleArray(intercept.size) { r ->
            var acc = intercept[r].toDouble()
            val row = coef[r]
            for (b in hist.indices) acc += row[b] * (hist[b] - mean[b]) / scale[b]
            acc
        }
    }
}
