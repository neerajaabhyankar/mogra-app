package com.mogralabs.mogra.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Window planning. The model scores whole 20 s windows and averages them, so how a
 * recording is cut into windows is a modelling decision, not a detail.
 */
class WindowingTest {

    private val sr = 16000

    private fun count(seconds: Double) =
        Resampler.windowCount((seconds * sr).toInt(), sr)

    @Test fun `a short recording is one window`() {
        assertEquals(1, count(12.0))
        assertEquals(1, count(20.0))
        assertEquals(1, count(25.0))
    }

    @Test fun `windows appear every fifteen seconds, not every twenty`() {
        assertEquals(2, count(35.0))
        assertEquals(3, count(50.0))
        assertEquals(4, count(65.0))
    }

    @Test fun `every window is exactly twenty seconds`() {
        val n = (47.0 * sr).toInt()
        val y = FloatArray(n) { it.toFloat() }
        val ws = Resampler.windows(y, sr)
        assertEquals(count(47.0), ws.size)
        ws.forEach { assertEquals(20 * sr, it.size) }
    }

    @Test fun `consecutive windows share five seconds of audio`() {
        val n = (50.0 * sr).toInt()
        val y = FloatArray(n) { it.toFloat() }            // the sample index is the value
        val ws = Resampler.windows(y, sr)
        assertTrue("expected at least two windows", ws.size >= 2)
        // the tail of one window is the head of the next, five seconds' worth
        val overlap = (Resampler.OVERLAP_SECONDS * sr).toInt()
        val tail = ws[0].copyOfRange(ws[0].size - overlap, ws[0].size)
        val head = ws[1].copyOfRange(0, overlap)
        assertEquals("overlap length", tail.size, head.size)
        for (i in tail.indices) {
            assertEquals("sample $i of the shared five seconds", tail[i], head[i], 0f)
        }
    }

    @Test fun `a recording is scored on its middle, not its start`() {
        // 24 s of audio, one window: it should be the middle 20 s, so the first sample of
        // the window is two seconds in
        val n = (24.0 * sr).toInt()
        val y = FloatArray(n) { it.toFloat() }
        val w = Resampler.windows(y, sr).single()
        assertEquals(2.0 * sr, w[0].toDouble(), 1.0)
    }
}
