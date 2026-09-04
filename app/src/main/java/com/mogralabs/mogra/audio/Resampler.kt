package com.mogralabs.mogra.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Rational polyphase resampling — a Kotlin transcription of `model/portable/frontend.py`.
 *
 * The phone records at 44.1 or 48 kHz and the two branches want 22.05 and 16 kHz. 48 → 22.05
 * is up=147 / down=320, and the textbook zero-stuff-then-filter form would build a
 * 65-million-sample intermediate for a single 20-second window. This is the polyphase form:
 * about 139 taps per output sample, no intermediate.
 */
object Resampler {

    private const val HALF_WIDTH = 32
    private const val KAISER_BETA = 14.769656459379492

    /** Cached per (srIn, srOut) — designing the filter is the expensive part, not applying it. */
    private val cache = HashMap<Long, Bank>()

    private class Bank(val up: Int, val down: Int, val phases: Array<DoubleArray>, val centre: Int) {
        val taps: Int get() = phases[0].size
    }

    @Synchronized
    private fun bankFor(srIn: Int, srOut: Int): Bank {
        val key = srIn.toLong() shl 32 or srOut.toLong()
        return cache.getOrPut(key) { design(srIn, srOut) }
    }

    private fun design(srIn: Int, srOut: Int): Bank {
        val g = gcd(srIn, srOut)
        val up = srOut / g
        val down = srIn / g
        val m = max(up, down)
        val cutoff = 0.5 / m * 0.99                 // 1 % guard below the new Nyquist
        val taps = 2 * HALF_WIDTH * m + 1

        val h = DoubleArray(taps)
        var sum = 0.0
        val i0beta = besselI0(KAISER_BETA)
        for (i in 0 until taps) {
            val t = i - (taps - 1) / 2.0
            val sinc = if (t == 0.0) 2.0 * cutoff else sin(2.0 * PI * cutoff * t) / (PI * t)
            val r = 2.0 * i / (taps - 1) - 1.0
            val w = besselI0(KAISER_BETA * sqrt(max(0.0, 1.0 - r * r))) / i0beta
            h[i] = sinc * w
            sum += h[i]
        }
        for (i in 0 until taps) h[i] = h[i] / sum * up

        val per = ceil(taps.toDouble() / up).toInt()
        val phases = Array(up) { p ->
            DoubleArray(per) { j -> val idx = p + j * up; if (idx < taps) h[idx] else 0.0 }
        }
        return Bank(up, down, phases, (taps - 1) / 2)
    }

    fun resample(y: FloatArray, srIn: Int, srOut: Int): FloatArray {
        if (srIn == srOut) return y.copyOf()
        val b = bankFor(srIn, srOut)
        val per = b.phases[0].size
        val nOut = ceil(y.size.toDouble() * b.up / b.down).toInt()
        val out = FloatArray(nOut)
        val shift = b.centre / b.up
        for (m in 0 until nOut) {
            val prod = m.toLong() * b.down
            val phase = (prod % b.up).toInt()
            val base = (prod / b.up).toInt() + shift
            val tap = b.phases[phase]
            var acc = 0.0
            for (j in 0 until per) {
                val idx = base - j
                if (idx in y.indices) acc += y[idx] * tap[j]
            }
            out[m] = acc.toFloat()
        }
        return out
    }

    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

    /** Zeroth-order modified Bessel function, series form — plenty for a window function. */
    private fun besselI0(x: Double): Double {
        var sum = 1.0
        var term = 1.0
        var k = 1
        while (k < 60) {
            term *= (x / 2.0) * (x / 2.0) / (k.toDouble() * k)
            sum += term
            if (term < 1e-18 * sum) break
            k++
        }
        return sum
    }

    /** Peak-normalise in place, as the CQT branch expects. Silence is left alone. */
    fun peakNormalise(y: FloatArray): FloatArray {
        var peak = 0f
        for (v in y) peak = max(peak, abs(v))
        if (peak == 0f) return y
        val g = 1f / peak
        return FloatArray(y.size) { y[it] * g }
    }

    /** Centre-crop or centre-pad to exactly [n] samples, as the training loader did. */
    fun fitLength(y: FloatArray, n: Int): FloatArray {
        if (y.size == n) return y
        val out = FloatArray(n)
        if (y.size < n) {
            val pad = (n - y.size) / 2
            System.arraycopy(y, 0, out, pad, y.size)
        } else {
            System.arraycopy(y, (y.size - n) / 2, out, 0, n)
        }
        return out
    }

    /**
     * Windows overlap by [OVERLAP_SECONDS], so a phrase that straddles a boundary is still
     * seen whole by one of them. Consecutive windows therefore start 15 s apart, not 20.
     */
    const val OVERLAP_SECONDS = 5.0

    private fun windowPlan(samples: Int, sr: Int, seconds: Double): Triple<Int, Int, Int> {
        val n = Math.round(sr * seconds).toInt()
        val hop = Math.round(sr * (seconds - OVERLAP_SECONDS)).toInt().coerceAtLeast(1)
        val k = max(1, Math.round((samples - n).toDouble() / hop).toInt() + 1)
        val span = n + (k - 1) * hop
        return Triple(k, hop, max(0, (samples - span) / 2))
    }

    /** How many windows [windows] would return, without building them. */
    fun windowCount(samples: Int, sr: Int, seconds: Double = 20.0): Int =
        windowPlan(samples, sr, seconds).first

    /**
     * Split a recording into overlapping [seconds]-long windows, **centred** on it.
     *
     * Every training example was one 20 s clip centre-cropped from a longer one, so a 24 s
     * recording is scored on its middle 20 s, not its first 20 s. The window count is the
     * length rounded to the nearest whole hop; a final window that runs off the end is
     * centre-padded, as the training loader did.
     */
    fun windows(y: FloatArray, sr: Int, seconds: Double = 20.0): List<FloatArray> {
        val n = Math.round(sr * seconds).toInt()
        val (k, hop, start) = windowPlan(y.size, sr, seconds)
        return (0 until k).map { i ->
            val from = (start + i * hop).coerceAtMost(y.size)
            val to = minOf(y.size, from + n)
            fitLength(if (from >= to) FloatArray(0) else y.copyOfRange(from, to), n)
        }
    }
}
