package com.mogralabs.mogra.audio

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/**
 * The binary assets, parsed by the same code the app uses.
 *
 * These files are produced by a Python script and read by Kotlin; nothing else checks that
 * the two agree on the layout, and a silent misparse here would look like a bad model
 * rather than a bad reader.
 */
class AssetParsingTest {

    private val assets = File("src/main/assets/model")
    private val goldens = File("src/test/resources/golden")

    private fun floats(f: File): FloatArray {
        val bytes = f.readBytes()
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(bytes.size / 4) { bb.float }
    }

    private fun meta() = JSONObject(File(goldens, "meta.json").readText())

    @Test fun `linear model parses and reproduces python scores`() {
        val m = RaagIdentifier.readLinear(File(assets, "melody_linear.bin").readBytes())
        assertEquals(meta().getInt("linear_n_bins"), m.mean.size)
        assertEquals(meta().getInt("linear_n_raags"), m.intercept.size)

        val hist = floats(File(goldens, "histogram.bin"))
        val want = floats(File(goldens, "linear_scores.bin"))
        val got = m.scores(hist)
        var worst = 0.0
        for (i in want.indices) worst = maxOf(worst, abs(got[i] - want[i]))
        assertTrue("linear scores max|d| = $worst", worst < 1e-4)
    }

    @Test fun `dithered bin centres parse`() {
        val c = RaagIdentifier.readBinCents(File(assets, "bin_cents.bin").readBytes())
        assertEquals(Melody.PITCH_BINS, c.size)
        // the dither is +-20 cents on a 20-cent grid starting at 1997.379
        for (b in c.indices) {
            val nominal = 20.0 * b + 1997.3794084376191
            assertTrue("bin $b: ${c[b]} vs nominal $nominal", abs(c[b] - nominal) <= 20.0001)
        }
    }

    @Test fun `raag list and config are the ones the model was calibrated with`() {
        val raags = org.json.JSONArray(File(assets, "raags.json").readText())
        assertEquals(50, raags.length())
        val cfg = JSONObject(File(assets, "config.json").readText())
        assertEquals(0.4, cfg.getDouble("melody_weight"), 1e-9)
        assertTrue(cfg.getDouble("temperature_cqt") > 0)
        assertTrue(cfg.getDouble("temperature_melody") > 0)
    }

    @Test fun `exported nets are present and not empty`() {
        listOf("cqt_net.ptl", "crepe_tiny.ptl").forEach {
            val f = File(assets, it)
            assertTrue("$it missing", f.exists())
            assertTrue("$it suspiciously small: ${f.length()}", f.length() > 1_000_000)
        }
    }
}
