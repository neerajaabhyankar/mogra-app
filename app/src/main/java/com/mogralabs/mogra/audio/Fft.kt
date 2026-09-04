package com.mogralabs.mogra.audio

/**
 * Iterative radix-2 FFT, in place, on separate real and imaginary arrays.
 *
 * The CQT needs a 16384- or 32768-point transform per 46 ms of audio and there are 431 of
 * them in a window, so this is the hot loop of the whole identifier. Kept deliberately
 * plain: no object allocation inside the transform, twiddles precomputed per size and
 * cached, arrays reused by the caller.
 */
class Fft(val n: Int) {

    init {
        require(n > 0 && n and (n - 1) == 0) { "FFT size must be a power of two, got $n" }
    }

    private val levels = Integer.numberOfTrailingZeros(n)
    private val cos = DoubleArray(n / 2) { Math.cos(2.0 * Math.PI * it / n) }
    private val sin = DoubleArray(n / 2) { Math.sin(2.0 * Math.PI * it / n) }

    /** In-place complex forward transform. */
    fun transform(re: DoubleArray, im: DoubleArray) {
        // bit-reversal permutation
        for (i in 0 until n) {
            val j = Integer.reverse(i) ushr (32 - levels)
            if (j > i) {
                var t = re[i]; re[i] = re[j]; re[j] = t
                t = im[i]; im[i] = im[j]; im[j] = t
            }
        }
        var size = 2
        while (size <= n) {
            val half = size / 2
            val step = n / size
            var i = 0
            while (i < n) {
                var j = i
                var k = 0
                while (j < i + half) {
                    val l = j + half
                    val tre = re[l] * cos[k] + im[l] * sin[k]
                    val tim = -re[l] * sin[k] + im[l] * cos[k]
                    re[l] = re[j] - tre
                    im[l] = im[j] - tim
                    re[j] += tre
                    im[j] += tim
                    j++
                    k += step
                }
                i += size
            }
            if (size == n) break
            size *= 2
        }
    }

    /**
     * Forward transform of real input, writing the non-negative frequencies (n/2 + 1 bins)
     * into [outRe]/[outIm]. `scratch*` must each be length [n].
     */
    fun realForward(
        x: DoubleArray, offset: Int, length: Int,
        scratchRe: DoubleArray, scratchIm: DoubleArray,
        outRe: DoubleArray, outIm: DoubleArray,
    ) {
        java.util.Arrays.fill(scratchRe, 0.0)
        java.util.Arrays.fill(scratchIm, 0.0)
        System.arraycopy(x, offset, scratchRe, 0, length)
        transform(scratchRe, scratchIm)
        System.arraycopy(scratchRe, 0, outRe, 0, n / 2 + 1)
        System.arraycopy(scratchIm, 0, outIm, 0, n / 2 + 1)
    }

    companion object {
        private val cache = HashMap<Int, Fft>()

        @Synchronized
        fun of(n: Int): Fft = cache.getOrPut(n) { Fft(n) }

        fun nextPowerOfTwo(v: Int): Int {
            var n = 1
            while (n < v) n = n shl 1
            return n
        }
    }
}
