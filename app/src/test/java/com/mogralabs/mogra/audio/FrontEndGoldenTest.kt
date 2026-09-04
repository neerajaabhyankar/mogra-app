package com.mogralabs.mogra.audio

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * The Kotlin front end against vectors produced by `model/portable`, the Python that scored
 * 0.4800 / 0.8200 on the 150-clip test set.
 *
 * These are not smoke tests. The whole argument for on-device inference is that the
 * transcription computes the same numbers as the thing that was measured, and this is where
 * that claim is either true or false.
 */
class FrontEndGoldenTest {

    private fun golden(name: String): FloatArray {
        val bytes = checkNotNull(javaClass.classLoader!!.getResourceAsStream("golden/$name"))
            .use { it.readBytes() }
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(bytes.size / 4) { bb.float }
    }

    private fun meta(): JSONObject = JSONObject(
        checkNotNull(javaClass.classLoader!!.getResourceAsStream("golden/meta.json"))
            .use { it.readBytes() }.decodeToString()
    )

    /** Mirrors `signal()` in model/export_goldens.py, LCG and all. */
    private fun signal(n: Int, sr: Int = 48000): FloatArray {
        var s = 12345L
        return FloatArray(n) { i ->
            val t = i.toDouble()
            val y = 0.6 * sin(2 * PI * 220.0 * t / sr) +
                    0.3 * sin(2 * PI * 440.0 * t / sr) +
                    0.1 * sin(2 * PI * 660.0 * t / sr)
            s = (s * 1103515245L + 12345L) and 0x7FFFFFFFL
            (y + 0.02 * (s.toDouble() / 0x7FFFFFFF * 2.0 - 1.0)).toFloat()
        }
    }

    private fun compare(tag: String, got: FloatArray, want: FloatArray, tol: Float) {
        assertEquals("$tag: length", want.size, got.size)
        var worst = 0f
        var at = 0
        var sum = 0.0
        var over = 0
        for (i in want.indices) {
            val d = abs(got[i] - want[i])
            sum += d
            if (d > tol) over++
            if (d > worst) { worst = d; at = i }
        }
        val mean = sum / want.size
        assertTrue(
            "$tag: max|d| = $worst at $at, mean|d| = $mean, " +
                "$over/${want.size} over $tol (want[$at] = ${want[at]}, got[$at] = ${got[at]})",
            worst <= tol,
        )
    }

    @Test fun `resampler matches python at 22050`() {
        compare("48k->22.05k", Resampler.resample(signal(48000), 48000, 22050),
            golden("resample_48_22050.bin"), 1e-6f)
    }

    @Test fun `resampler matches python at 16000`() {
        compare("48k->16k", Resampler.resample(signal(48000), 48000, 16000),
            golden("resample_48_16000.bin"), 1e-6f)
    }

    @Test fun `half precision round trip matches numpy float16`() {
        val cases = meta().getJSONArray("f16_cases")
        val inputs = listOf(0.0f, 1.0f, -1.0f, 0.1f, -0.1f, 79.9f, -79.9f, 1e-5f, 65504.0f, 0.33333333f)
        inputs.forEachIndexed { i, v ->
            assertEquals("f16($v)", cases.getDouble(i).toFloat(), Cqt.f16(v), 0f)
        }
    }

    @Test fun `anchor fmin folds the tonic the same way`() {
        val m = meta()
        assertEquals(m.getDouble("anchor_fmin"), Cqt.anchorFmin(m.getDouble("tonic_hz")), 1e-9)
    }

    @Test fun `cqt features match the trained front end`() {
        val m = meta()
        val twenty = signal(48000 * 20)
        var y22 = Resampler.resample(twenty, 48000, Cqt.SR)
        y22 = Resampler.peakNormalise(y22)
        y22 = Resampler.fitLength(y22, 441000)
        val got = Cqt.features(y22, Cqt.Kernel.forTonic(m.getDouble("tonic_hz")))
        // the feature lives in [0, 1]; 1e-3 is well inside the float16 quantisation it
        // already went through, and two orders below the 0.002 mean gap to librosa itself
        compare("cqt", got, golden("cqt_features.bin"), 1e-3f)
    }

    @Test fun `pitch histogram matches python`() {
        val tonic = meta().getDouble("tonic_hz")
        val n = 500
        val f0 = FloatArray(n) { i ->
            (tonic * Math.pow(2.0, (i % 12) / 12.0) * (1 + 0.01 * sin(i / 7.0))).toFloat()
        }
        val voiced = BooleanArray(n) { it % 5 != 0 }
        compare("histogram", Melody.histogram(f0, voiced, tonic), golden("histogram.bin"), 1e-6f)
    }

    @Test fun `yin reads a held hum`() {
        val m = meta()
        val sr = 22050
        val n = (4.0 * sr).toInt()
        var phase = 0.0
        val y = FloatArray(n) { i ->
            val f = 146.83 * (1 + 0.008 * sin(2 * PI * 5.2 * i / sr))
            phase += 2 * PI * f / sr
            var v = 0.0
            listOf(1.0, .5, .3, .18, .1).forEachIndexed { k, a -> v += a * sin((k + 1) * phase) }
            v.toFloat()
        }
        val got = Yin.fromHum(y, sr)
        val want = m.getDouble("hum_expected_hz")
        val cents = 1200 * (Math.log(got / want) / Math.log(2.0))
        assertTrue("hum: got $got Hz, python says $want Hz (${"%.1f".format(cents)} cents)",
            abs(cents) < 5.0)
    }
}
