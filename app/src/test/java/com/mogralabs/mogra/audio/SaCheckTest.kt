package com.mogralabs.mogra.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.exp

/**
 * Checking a given Sa against the recording's own pitch histogram.
 *
 * The three outcomes are the point, and two of them are silence: a singer who never touches
 * Sa must not be warned, and one who is a few cents off must not be nagged.
 */
class SaCheckTest {

    /** 120 bins of 10 cents, with Gaussian bumps where the singer dwelt. */
    private fun histogram(vararg peaksAtCents: Pair<Double, Double>): DoubleArray {
        val h = DoubleArray(120)
        for (b in 0 until 120) {
            val cents = b * 10.0
            for ((peak, weight) in peaksAtCents) {
                var d = cents - peak
                while (d > 600) d -= 1200
                while (d < -600) d += 1200
                h[b] += weight * exp(-0.5 * (d / 18.0) * (d / 18.0))
            }
        }
        return h
    }

    @Test fun `a singer sitting on Sa reports no drift`() {
        val offset = SaCheck.offsetCents(histogram(0.0 to 1.0, 700.0 to 0.8))
        assertTrue("expected a peak near Sa, got $offset", offset != null && abs(offset) < 5)
        assertFalse(SaCheck.isDrift(offset))
    }

    @Test fun `a small offset is detected but not worth reporting`() {
        val small = SaCheck.offsetCents(histogram(8.0 to 1.0))!!
        assertTrue("expected about 8 cents, got $small", abs(small) <= SaCheck.IGNORE_CENTS + 3)
        assertFalse(SaCheck.isDrift(small))
    }

    @Test fun `a peak beyond the band is a different swar, not a drifted Sa`() {
        // 60 cents from the tonic is on the way to komal Re; nothing to warn about
        assertNull(SaCheck.offsetCents(histogram(60.0 to 1.0)))
    }

    @Test fun `a drifted Sa is reported`() {
        val sharp = SaCheck.offsetCents(histogram(30.0 to 1.0, 730.0 to 0.7))!!
        assertEquals(30.0, sharp, 6.0)
        assertTrue(SaCheck.isDrift(sharp))

        val flat = SaCheck.offsetCents(histogram(-30.0 to 1.0))!!
        assertEquals(-30.0, flat, 6.0)
        assertTrue(SaCheck.isDrift(flat))
    }

    @Test fun `a singer who never touches Sa is left alone`() {
        // N R G M D N D P in Yaman: everything but Sa. Nothing within 50 cents of the tonic.
        val yamanWithoutSa = histogram(
            200.0 to 1.0, 400.0 to 1.0, 600.0 to 0.9, 900.0 to 1.0, 1100.0 to 0.8)
        assertNull(SaCheck.offsetCents(yamanWithoutSa))
        assertFalse(SaCheck.isDrift(null))
    }

    @Test fun `the fold is circular, so a flat Sa is not a sharp one`() {
        // a peak at 1180 cents is 20 cents *below* Sa, not 1180 above
        val offset = SaCheck.offsetCents(histogram(1180.0 to 1.0))!!
        assertEquals(-20.0, offset, 6.0)
    }

    @Test fun `an empty or flat histogram says nothing`() {
        assertNull(SaCheck.offsetCents(DoubleArray(120)))
        assertNull(SaCheck.offsetCents(DoubleArray(0)))
    }

    @Test fun `the corrected tonic moves by the offset`() {
        assertEquals(220.0, SaCheck.correctedTonic(220.0, 0.0), 1e-9)
        // a hundred cents is a semitone
        assertEquals(220.0 * Math.pow(2.0, 1 / 12.0),
            SaCheck.correctedTonic(220.0, 100.0), 1e-9)
        assertTrue(SaCheck.correctedTonic(220.0, -30.0) < 220.0)
    }
}
