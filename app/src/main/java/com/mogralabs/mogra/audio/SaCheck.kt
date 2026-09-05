package com.mogralabs.mogra.audio

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Did the singer's Sa land where they said it would?
 *
 * The idea is lifted from `annotate_tonics.py`, which snaps a hummed tonic onto a peak of
 * the recording's own pitch histogram: the recording states its Sa far more precisely than
 * a hum does. Here it is used to *check* rather than to snap — the tonic the user gave is
 * still what the model was run with, and this only reports whether the recording disagrees.
 *
 * The histogram is the one the melody branch already builds: 120 bins of 10 cents, folded
 * into one octave, with bin 0 sitting on Sa. Nothing extra is computed.
 *
 * Three outcomes, and two of them are silence:
 *
 *  * **no peak within 50 cents** — the singer never dwelt near Sa at all. Perfectly normal;
 *    `N R G M D N D P` is a legitimate Yaman phrase with no Sa in it. Say nothing.
 *  * **a peak within 10 cents** — they are where they said they are. Say nothing.
 *  * **a peak between 10 and 50 cents away** — the Sa may have drifted, and the result may
 *    be worth re-running. Worth one quiet warning.
 */
object SaCheck {

    /** Closer than this and the difference cannot be heard, let alone acted on. */
    const val IGNORE_CENTS = 10.0

    /** Further than this and the peak is a different swar, not a drifted Sa. */
    const val MAX_CENTS = 50.0

    private const val BIN_CENTS = 10.0

    /** Peaks below this share of the tallest are histogram noise. */
    private const val MIN_PROMINENCE = 0.25

    /**
     * How far the nearest sung peak sits from the given Sa, in cents, or null when nothing
     * is near enough to be Sa at all. Positive means the recording is sharp of the tonic.
     */
    fun offsetCents(histogram: DoubleArray): Double? {
        val n = histogram.size
        if (n < 3) return null
        val max = histogram.max()
        if (max <= 0.0) return null

        var best: Double? = null
        for (i in 0 until n) {
            val left = histogram[(i - 1 + n) % n]
            val here = histogram[i]
            val right = histogram[(i + 1) % n]
            if (here < left || here <= right || here < MIN_PROMINENCE * max) continue

            // parabolic interpolation, so the answer is not pinned to the 10-cent grid
            val denom = left - 2 * here + right
            val delta = if (denom == 0.0) 0.0 else (0.5 * (left - right) / denom).coerceIn(-1.0, 1.0)
            val cents = wrap((i + delta) * BIN_CENTS)
            if (abs(cents) <= MAX_CENTS && (best == null || abs(cents) < abs(best!!))) best = cents
        }
        return best
    }

    /** Whether an offset is worth telling the user about. */
    fun isDrift(offsetCents: Double?): Boolean =
        offsetCents != null && abs(offsetCents) > IGNORE_CENTS && abs(offsetCents) <= MAX_CENTS

    /** The tonic the recording seems to be using, given the one that was supplied. */
    fun correctedTonic(tonicHz: Double, offsetCents: Double): Double =
        tonicHz * Math.pow(2.0, offsetCents / 1200.0)

    fun roundedCents(offsetCents: Double): Int = abs(offsetCents).roundToInt()

    /** An octave-folded histogram wraps, so 1190 cents is 10 cents flat, not 1190 sharp. */
    private fun wrap(cents: Double): Double {
        var c = cents % 1200.0
        if (c > 600.0) c -= 1200.0
        if (c < -600.0) c += 1200.0
        return c
    }
}
