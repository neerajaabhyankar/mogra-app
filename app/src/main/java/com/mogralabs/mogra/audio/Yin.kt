package com.mogralabs.mogra.audio

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Reading Sa out of a few seconds of held hum — a transcription of `model/portable/hum.py`.
 *
 * The reference uses pYIN: YIN plus an HMM over pitch candidates. The HMM is there to hold
 * a *melody* together through leaps and octave errors, and a held Sa has no leaps, so this
 * is plain YIN with a voicing threshold and the same median. The median is the part that
 * does the work — a hum starts and ends with a scoop, and the median ignores both instead
 * of averaging them into the answer.
 *
 * The octave check at the end is the one thing pYIN's HMM was giving for free: YIN's
 * cumulative-mean normalisation can settle on a period twice the true one when the second
 * harmonic is strong, which for a hummed Sa is a real risk.
 */
object Yin {

    private const val FRAME = 2048
    private const val HOP = 256
    const val FMIN = 60.0
    const val FMAX = 600.0
    private const val THRESHOLD = 0.15
    private const val VOICED_MAX = 0.45

    class Result(val f0: DoubleArray, val voiced: BooleanArray)

    fun track(y: FloatArray, sr: Int, fmin: Double = FMIN, fmax: Double = FMAX): Result {
        val tauMin = maxOf(2, floor(sr / fmax).toInt())
        val tauMax = minOf(FRAME - 1, ceil(sr / fmin).toInt() + 1)
        val padded = if (y.size >= FRAME) y else y.copyOf(FRAME)

        val f0 = ArrayList<Double>()
        val voiced = ArrayList<Boolean>()
        val d = DoubleArray(tauMax)
        var start = 0
        while (start + FRAME <= padded.size) {
            difference(padded, start, tauMax, d)
            var tau = -1
            for (t in tauMin until tauMax) if (d[t] < THRESHOLD) { tau = t; break }
            if (tau < 0) {
                tau = tauMin
                for (t in tauMin until tauMax) if (d[t] < d[tau]) tau = t
            } else {
                while (tau + 1 < tauMax && d[tau + 1] < d[tau]) tau++
            }
            f0.add(sr / parabolic(d, tau))
            voiced.add(d[tau] < VOICED_MAX)
            start += HOP
        }
        return Result(f0.toDoubleArray(), voiced.toBooleanArray())
    }

    /** A few seconds of a held Sa -> its frequency in Hz. */
    fun fromHum(y: FloatArray, sr: Int, fmin: Double = FMIN, fmax: Double = FMAX): Double {
        val r = track(y, sr, fmin, fmax)
        val kept = ArrayList<Double>(r.f0.size)
        for (i in r.f0.indices) if (r.voiced[i] && r.f0[i].isFinite()) kept.add(r.f0[i])
        require(kept.size >= 10) {
            "could not hear a steady pitch -- hum one note, louder, for ~5 s"
        }
        kept.sort()
        val med = if (kept.size % 2 == 1) kept[kept.size / 2]
                  else (kept[kept.size / 2 - 1] + kept[kept.size / 2]) / 2.0

        // if a third of the frames sit within 5 % of half the median period, YIN locked
        // onto a subharmonic and the true Sa is the octave above
        if (fmax >= 2 * med) {
            var near = 0
            for (v in kept) if (abs(v / (2 * med) - 1.0) < 0.05) near++
            if (near.toDouble() / kept.size > 0.33) return 2 * med
        }
        return med
    }

    /** YIN's cumulative mean normalised difference function, computed directly. */
    private fun difference(y: FloatArray, offset: Int, tauMax: Int, out: DoubleArray) {
        out[0] = 1.0
        var running = 0.0
        for (tau in 1 until tauMax) {
            var sum = 0.0
            var i = 0
            val n = FRAME - tau
            while (i < n) {
                val diff = y[offset + i] - y[offset + i + tau]
                sum += diff.toDouble() * diff
                i++
            }
            running += sum
            out[tau] = if (running > 0) sum * tau / running else 1.0
        }
    }

    private fun parabolic(d: DoubleArray, tau: Int): Double {
        if (tau <= 0 || tau >= d.size - 1) return tau.toDouble()
        val a = d[tau - 1]; val b = d[tau]; val c = d[tau + 1]
        val denom = a - 2 * b + c
        return if (denom == 0.0) tau.toDouble() else tau + 0.5 * (a - c) / denom
    }
}
