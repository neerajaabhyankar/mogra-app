package com.mogralabs.mogra.identifier

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Microphone in, tone out. Both deliberately small: the interesting arithmetic happens in
 * `com.mogralabs.mogra.audio`, and this only has to deliver float PCM to it.
 */
object Audio {

    /** 48 kHz where the device allows it, else 44.1 — both resample cleanly downstream. */
    val CANDIDATE_RATES = intArrayOf(48000, 44100, 22050, 16000)

    class Recording(val samples: FloatArray, val sampleRate: Int) {
        val seconds: Double get() = samples.size.toDouble() / sampleRate
    }

    /**
     * Record until the caller's coroutine is cancelled, reporting a 0..1 level as it goes.
     *
     * The level is a peak over each read rather than an RMS: it is driving a meter that
     * tells someone whether the phone can hear them, and a peak answers that question
     * sooner.
     */
    @SuppressLint("MissingPermission")
    suspend fun record(
        maxSeconds: Double = 120.0,
        shouldStop: () -> Boolean = { false },
        onLevel: (Float) -> Unit = {},
        onElapsed: (Double) -> Unit = {},
    ): Recording = withContext(Dispatchers.IO) {
        val sr = CANDIDATE_RATES.firstOrNull { rate ->
            AudioRecord.getMinBufferSize(
                rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT,
            ) > 0
        } ?: 44100

        val minBuf = AudioRecord.getMinBufferSize(
            sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        val bufBytes = max(minBuf, sr * 4)                  // a second of headroom
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.UNPROCESSED.takeIf { supportsUnprocessed() }
                ?: MediaRecorder.AudioSource.MIC,
            sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT, bufBytes,
        )
        check(recorder.state == AudioRecord.STATE_INITIALIZED) { "microphone unavailable" }

        val out = ArrayList<FloatArray>()
        var total = 0
        val chunk = FloatArray(sr / 10)                     // 100 ms
        try {
            recorder.startRecording()
            while (coroutineContext.isActive && !shouldStop() && total < maxSeconds * sr) {
                val n = recorder.read(chunk, 0, chunk.size, AudioRecord.READ_BLOCKING)
                if (n <= 0) continue
                out.add(chunk.copyOf(n))
                total += n
                var peak = 0f
                for (i in 0 until n) peak = max(peak, abs(chunk[i]))
                onLevel(min(1f, peak))
                onElapsed(total.toDouble() / sr)
            }
        } finally {
            runCatching { recorder.stop() }
            recorder.release()
        }

        val samples = FloatArray(total)
        var at = 0
        for (part in out) { System.arraycopy(part, 0, samples, at, part.size); at += part.size }
        Recording(samples, sr)
    }

    /** UNPROCESSED skips the phone's voice-call AGC and noise suppression, which mangle music. */
    private fun supportsUnprocessed(): Boolean =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N

    /**
     * A short drone at [hz] so the user can hear the Sa they just chose.
     *
     * Three partials at falling amplitude rather than a bare sine — a sine is hard to sing
     * against, and the octave and fifth are what a tanpura gives you anyway.
     */
    suspend fun playTone(hz: Double, seconds: Double = 1.6) = withContext(Dispatchers.IO) {
        val sr = 44100
        val n = (sr * seconds).toInt()
        val pcm = FloatArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / sr
            val v = 0.6 * sin(2 * PI * hz * t) +
                    0.25 * sin(2 * PI * hz * 2 * t) +
                    0.12 * sin(2 * PI * hz * 3 * t)
            val attack = min(1.0, t / 0.04)
            val release = min(1.0, (seconds - t) / 0.25)
            val body = exp(-t * 0.6)
            pcm[i] = (v * attack * max(0.0, release) * body * 0.5).toFloat()
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sr)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build())
            .setBufferSizeInBytes(n * 4)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        try {
            track.write(pcm, 0, n, AudioTrack.WRITE_BLOCKING)
            track.play()
            kotlinx.coroutines.delay((seconds * 1000).toLong())
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }

}
