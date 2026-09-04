package com.mogralabs.mogra.audio

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** The CQT taken apart: FFT, then filter bank, then spectrum, then the projection. */
class CqtPiecesTest {

    private fun golden(name: String): FloatArray {
        val bytes = checkNotNull(javaClass.classLoader!!.getResourceAsStream("golden/$name"))
            .use { it.readBytes() }
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(bytes.size / 4) { bb.float }
    }

    private fun meta() = JSONObject(
        checkNotNull(javaClass.classLoader!!.getResourceAsStream("golden/meta.json"))
            .use { it.readBytes() }.decodeToString()
    )

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

    /** The FFT against a literal DFT — if this is wrong nothing above it can be right. */
    @Test fun `fft agrees with a direct dft`() {
        val n = 64
        val re = DoubleArray(n) { sin(2 * PI * 5 * it / n) + 0.3 * cos(2 * PI * 11 * it / n) }
        val im = DoubleArray(n)
        val x = re.copyOf()
        Fft.of(n).transform(re, im)
        var worst = 0.0
        for (k in 0 until n) {
            var dr = 0.0; var di = 0.0
            for (t in 0 until n) {
                val a = -2 * PI * k * t / n
                dr += x[t] * cos(a); di += x[t] * sin(a)
            }
            worst = maxOf(worst, abs(dr - re[k]), abs(di - im[k]))
        }
        assertTrue("fft vs dft max|d| = $worst", worst < 1e-9)
    }

    @Test fun `kernel geometry matches`() {
        val k = Cqt.build(meta().getDouble("anchor_fmin"))
        assertEquals(meta().getInt("n_fft"), k.nFft)
        val lengths = golden("kernel_lengths.bin")
        for (i in lengths.indices) {
            assertEquals("lengths[$i]", lengths[i].toDouble(), k.lengths[i], 1e-2)
        }
    }

    @Test fun `kernel row 52 matches`() {
        val k = Cqt.build(meta().getDouble("anchor_fmin"))
        val want = golden("kernel_row52.bin")          // re, im interleaved, dense
        val half = k.nFft / 2 + 1
        val gotRe = DoubleArray(half)
        val gotIm = DoubleArray(half)
        k.idx[52].forEachIndexed { j, c -> gotRe[c] = k.re[52][j]; gotIm[c] = k.im[52][j] }
        var worst = 0.0
        var at = -1
        for (c in 0 until half) {
            val d = maxOf(abs(gotRe[c] - want[2 * c]), abs(gotIm[c] - want[2 * c + 1]))
            if (d > worst) { worst = d; at = c }
        }
        assertTrue("kernel row 52: max|d| = $worst at bin $at " +
            "(want ${want[2 * at]} + ${want[2 * at + 1]}i, got ${gotRe[at]} + ${gotIm[at]}i, " +
            "nnz kotlin ${k.idx[52].size}, python ${meta().getInt("kernel_nnz_row52")})",
            // the golden is stored as float32, so ~1e-8 is the floor here, not an error
            worst < 1e-6)
    }

    @Test fun `stft frame 0 matches`() {
        val twenty = signal(48000 * 20)
        val y22 = Resampler.fitLength(
            Resampler.peakNormalise(Resampler.resample(twenty, 48000, Cqt.SR)), 441000)
        val nFft = meta().getInt("n_fft")
        val fft = Fft.of(nFft)
        val padded = DoubleArray(y22.size + nFft)
        for (i in y22.indices) padded[i + nFft / 2] = y22[i].toDouble()
        val half = nFft / 2 + 1
        val sre = DoubleArray(nFft); val sim = DoubleArray(nFft)
        val outRe = DoubleArray(half); val outIm = DoubleArray(half)
        fft.realForward(padded, 0, nFft, sre, sim, outRe, outIm)
        val want = golden("stft_frame0.bin")
        var worst = 0.0; var at = -1
        for (c in 0 until half) {
            val d = maxOf(abs(outRe[c] - want[2 * c]), abs(outIm[c] - want[2 * c + 1]))
            if (d > worst) { worst = d; at = c }
        }
        assertTrue("stft frame 0: max|d| = $worst at bin $at", worst < 1e-3)
    }
}
