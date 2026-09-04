package com.mogralabs.mogra.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The Sa-anchored constant-Q transform the first branch was trained on — a Kotlin
 * transcription of `model/portable/frontend.py`.
 *
 * Bin 0 is Sa itself, one octave below the tonic folded into [110, 220). That mapping is
 * the same for every recording ever made, which is why the network never had to learn
 * transposition. 36 bins per octave, four octaves, 1024-sample hop at 22.05 kHz.
 *
 * The filter bank depends only on Sa, so [Kernel.forTonic] is built once when the user sets
 * their tonic and reused for every recording after that.
 */
object Cqt {

    const val SR = 22050
    const val BINS_PER_OCTAVE = 36
    const val OCTAVES = 4
    const val N_BINS = BINS_PER_OCTAVE * OCTAVES
    const val HOP = 1024
    const val N_FRAMES = 431
    const val WINDOW_SECONDS = 20.0
    private const val TOP_DB = 80.0
    private const val SPARSITY = 0.01

    /** Sa folded into [lo, 2*lo). A raag is octave-invariant, so this makes that exact. */
    fun canonical(tonicHz: Double, lo: Double = 110.0): Double {
        var f = tonicHz
        while (f < lo) f *= 2.0
        while (f >= 2.0 * lo) f /= 2.0
        return f
    }

    /** The CQT `fmin` that puts Sa on bin 0, one octave below the canonical Sa. */
    fun anchorFmin(tonicHz: Double, octavesBelow: Int = 1, lo: Double = 110.0): Double =
        canonical(tonicHz, lo) / 2.0.pow(octavesBelow)

    /**
     * The sparse frequency-domain filter bank. Rows are stored as index/value triples
     * because sparsifying leaves roughly 5 000 non-zeros out of 144 × 8193.
     */
    class Kernel(
        val nFft: Int,
        val lengths: DoubleArray,
        val idx: Array<IntArray>,
        val re: Array<DoubleArray>,
        val im: Array<DoubleArray>,
    ) {
        companion object {
            fun forTonic(tonicHz: Double, sr: Int = SR): Kernel = build(anchorFmin(tonicHz), sr)
        }
    }

    fun build(fmin: Double, sr: Int = SR): Kernel {
        val alpha = (2.0.pow(2.0 / BINS_PER_OCTAVE) - 1) / (2.0.pow(2.0 / BINS_PER_OCTAVE) + 1)
        val q = 1.0 / alpha
        val freqs = DoubleArray(N_BINS) { fmin * 2.0.pow(it.toDouble() / BINS_PER_OCTAVE) }
        val lengths = DoubleArray(N_BINS) { q * sr / freqs[it] }
        val nFft = Fft.nextPowerOfTwo(Math.ceil(lengths.max()).toInt())
        val fft = Fft.of(nFft)
        val half = nFft / 2 + 1

        val idx = arrayOfNulls<IntArray>(N_BINS)
        val kre = arrayOfNulls<DoubleArray>(N_BINS)
        val kim = arrayOfNulls<DoubleArray>(N_BINS)
        val bre = DoubleArray(nFft)
        val bim = DoubleArray(nFft)

        for (k in 0 until N_BINS) {
            val ilen = lengths[k].toInt() / 2
            val len = 2 * ilen
            java.util.Arrays.fill(bre, 0.0)
            java.util.Arrays.fill(bim, 0.0)

            // complex exponential at freqs[k], windowed by a periodic Hann, L1-normalised
            val start = (nFft - len) / 2
            var l1 = 0.0
            val tmpRe = DoubleArray(len)
            val tmpIm = DoubleArray(len)
            for (i in 0 until len) {
                val t = (-ilen + i).toDouble()
                val w = 0.5 - 0.5 * cos(2.0 * PI * i / len)
                val phase = t * 2.0 * PI * freqs[k] / sr
                tmpRe[i] = cos(phase) * w
                tmpIm[i] = sin(phase) * w
                l1 += Math.hypot(tmpRe[i], tmpIm[i])
            }
            val gain = lengths[k] / nFft / l1
            for (i in 0 until len) {
                bre[start + i] = tmpRe[i] * gain
                bim[start + i] = tmpIm[i] * gain
            }
            fft.transform(bre, bim)

            // keep the largest entries holding 99 % of the row's L1 mass
            val mag = DoubleArray(half) { Math.hypot(bre[it], bim[it]) }
            val order = (0 until half).sortedByDescending { mag[it] }
            var total = 0.0
            for (m in mag) total += m
            val limit = (1.0 - SPARSITY) * total
            var acc = 0.0
            val keep = ArrayList<Int>(256)
            for ((rank, j) in order.withIndex()) {
                acc += mag[j]
                if (rank > 0 && acc >= limit) break
                keep.add(j)
            }
            keep.sort()
            idx[k] = keep.toIntArray()
            kre[k] = DoubleArray(keep.size) { bre[keep[it]] }
            kim[k] = DoubleArray(keep.size) { bim[keep[it]] }
        }
        @Suppress("UNCHECKED_CAST")
        return Kernel(nFft, lengths,
            idx as Array<IntArray>, kre as Array<DoubleArray>, kim as Array<DoubleArray>)
    }

    /**
     * 20 s of peak-normalised 22.05 kHz audio → the (1, 144, 431) tensor the network wants,
     * flattened row-major.
     *
     * The float16 round-trip is not a storage decision: the reference front end does it
     * before the network sees the values, so the network was trained on numbers that had
     * been through it.
     */
    /**
     * Round a float through IEEE half precision and back.
     *
     * Written out rather than taken from `android.util.Half` so the DSP has no Android
     * dependency and can be tested on the JVM against the Python vectors.
     */
    fun f16(value: Float): Float {
        val bits = java.lang.Float.floatToIntBits(value)
        val sign = bits ushr 31
        var exp = (bits ushr 23) and 0xFF
        var mant = bits and 0x7FFFFF
        if (exp == 0xFF) return value                       // Inf / NaN survive unchanged
        var e = exp - 127 + 15
        if (e >= 0x1F) return if (sign == 1) Float.NEGATIVE_INFINITY else Float.POSITIVE_INFINITY
        if (e <= 0) {                                       // subnormal or zero
            if (e < -10) return if (sign == 1) -0.0f else 0.0f
            mant = mant or 0x800000
            val shift = 14 - e
            val half = (mant + (1 shl (shift - 1))) ushr shift
            return java.lang.Float.intBitsToFloat(sign shl 31) + (if (sign == 1) -1f else 1f) *
                half.toFloat() * 5.960464477539063e-8f
        }
        // round to nearest, ties to even, on the 10-bit mantissa
        val round = (mant + 0x1000 + ((mant ushr 13) and 1)) ushr 13
        if (round > 0x3FF) { e += 1; if (e >= 0x1F) return if (sign == 1) Float.NEGATIVE_INFINITY else Float.POSITIVE_INFINITY }
        val m10 = round and 0x3FF
        exp = e - 15 + 127
        return java.lang.Float.intBitsToFloat((sign shl 31) or (exp shl 23) or (m10 shl 13))
    }

    fun features(y22050: FloatArray, kernel: Kernel): FloatArray {
        val nFft = kernel.nFft
        val fft = Fft.of(nFft)
        val half = nFft / 2 + 1

        val padded = DoubleArray(y22050.size + nFft)
        for (i in y22050.indices) padded[i + nFft / 2] = y22050[i].toDouble()
        val nFrames = 1 + (padded.size - nFft) / HOP

        val scratchRe = DoubleArray(nFft)
        val scratchIm = DoubleArray(nFft)
        val specRe = DoubleArray(half)
        val specIm = DoubleArray(half)

        val db = Array(N_BINS) { DoubleArray(nFrames) }
        var maxPower = 0.0
        for (t in 0 until nFrames) {
            fft.realForward(padded, t * HOP, nFft, scratchRe, scratchIm, specRe, specIm)
            for (k in 0 until N_BINS) {
                val ii = kernel.idx[k]
                val kr = kernel.re[k]
                val ki = kernel.im[k]
                var accRe = 0.0
                var accIm = 0.0
                for (j in ii.indices) {
                    val c = ii[j]
                    accRe += kr[j] * specRe[c] - ki[j] * specIm[c]
                    accIm += kr[j] * specIm[c] + ki[j] * specRe[c]
                }
                val scale = sqrt(kernel.lengths[k])
                val power = (accRe * accRe + accIm * accIm) / (scale * scale)
                db[k][t] = power
                if (power > maxPower) maxPower = power
            }
        }

        val refDb = 10.0 * log10(max(1e-10, maxPower))
        var top = Double.NEGATIVE_INFINITY
        for (k in 0 until N_BINS) for (t in 0 until nFrames) {
            val v = 10.0 * log10(max(1e-10, db[k][t])) - refDb
            db[k][t] = v
            if (v > top) top = v
        }
        val floorDb = top - TOP_DB

        // float16 round-trip, then crop or pad to the trained 431 frames
        val out = FloatArray(N_BINS * N_FRAMES)
        var padValue = Float.MAX_VALUE
        val rounded = Array(N_BINS) { k ->
            FloatArray(nFrames) { t ->
                val v = f16(max(db[k][t], floorDb).toFloat())
                if (v < padValue) padValue = v
                v
            }
        }
        for (k in 0 until N_BINS) for (t in 0 until N_FRAMES) {
            val v = if (t < nFrames) rounded[k][t] else padValue
            out[k * N_FRAMES + t] = (v + 80.0f) / 80.0f
        }
        return out
    }
}
