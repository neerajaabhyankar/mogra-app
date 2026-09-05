package com.mogralabs.mogra.identifier

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.annotation.StringRes
import com.mogralabs.mogra.R
import com.mogralabs.mogra.audio.Cqt
import com.mogralabs.mogra.audio.RaagIdentifier
import com.mogralabs.mogra.audio.Resampler
import com.mogralabs.mogra.audio.SaCheck
import com.mogralabs.mogra.audio.Yin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

/**
 * The Raag Identifier flow: choose Sa, record, analyse, read the answer.
 *
 * The model is loaded once and kept; so is the CQT filter bank, which depends only on Sa
 * and costs more to build than a whole window costs to transform. Both are dropped and
 * rebuilt when the tonic changes.
 */
class IdentifierViewModel(app: Application) : AndroidViewModel(app) {

    enum class Step { SET_SA, RECORD, ANALYSING, RESULT }

    enum class SaTab { HUM, KEYBOARD, HZ }

    data class State(
        val step: Step = Step.SET_SA,
        val hasTake: Boolean = false,
        val saHz: Double = 138.59,
        val saTab: SaTab = SaTab.KEYBOARD,
        val keyboardMidi: Int = Sa.DEFAULT_MIDI,
        val listening: Boolean = false,
        val heardHz: Double? = null,
        val recording: Boolean = false,
        val elapsed: Double = 0.0,
        val level: Float = 0f,
        val windows: Int = 0,
        val analysedSeconds: Double = 0.0,
        val elapsedMillis: Long = 0,
        val predictions: List<RaagIdentifier.Prediction> = emptyList(),
        /** How far the recording's own Sa sits from the given one, when it is worth saying. */
        val saDriftCents: Double? = null,
        @StringRes val error: Int? = null,
    ) {
        val canAnalyse: Boolean get() = elapsed >= MIN_SECONDS
    }

    private val _state = MutableStateFlow(State(saHz = Sa.remembered(app)).let {
        it.copy(keyboardMidi = Sa.nearest(it.saHz).first)
    })
    val state: StateFlow<State> = _state.asStateFlow()

    private var model: RaagIdentifier? = null
    private var kernel: Cqt.Kernel? = null
    private var kernelFor: Double = Double.NaN
    private var captured: Audio.Recording? = null
    private var recordJob: Job? = null
    @Volatile private var stopRequested = false
    @Volatile private var humStopRequested = false
    private var listenJob: Job? = null
    private var analyseJob: Job? = null

    // ---------------------------------------------------------------- Sa

    fun selectTab(tab: SaTab) = _state.update { it.copy(saTab = tab, error = null) }

    fun selectMidi(midi: Int) = _state.update {
        it.copy(keyboardMidi = midi, saHz = Sa.hzOf(midi), heardHz = null)
    }

    fun setSaHz(hz: Double) {
        if (!hz.isFinite() || hz <= 0) return
        _state.update { it.copy(saHz = hz, keyboardMidi = Sa.nearest(hz).first) }
    }

    fun playSa() {
        viewModelScope.launch { runCatching { Audio.playTone(_state.value.saHz) } }
    }

    /** Listen for a held hum and take its median pitch as Sa. */
    fun listenForSa() {
        // tapping again stops early and still uses what was sung, the same way the raag
        // recording does -- cancelling the coroutine would throw the samples away and
        // surface as "StandaloneCoroutine was cancelled"
        if (_state.value.listening) { humStopRequested = true; return }
        humStopRequested = false
        _state.update { it.copy(listening = true, heardHz = null, error = null) }
        listenJob = viewModelScope.launch {
            runCatching {
                val rec = Audio.record(
                    maxSeconds = 8.0,
                    shouldStop = { humStopRequested },
                    onLevel = { lv -> _state.update { it.copy(level = lv) } },
                )
                withContext(Dispatchers.Default) { Yin.fromHum(rec.samples, rec.sampleRate) }
            }.onSuccess { hz ->
                _state.update {
                    it.copy(listening = false, level = 0f, heardHz = hz, saHz = hz,
                        keyboardMidi = Sa.nearest(hz).first)
                }
            }.onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.update {
                    it.copy(listening = false, level = 0f, error = errorFor(e, R.string.err_no_pitch))
                }
            }
        }
    }

    fun confirmSa() {
        Sa.remember(getApplication(), _state.value.saHz)
        _state.update { it.copy(step = Step.RECORD, error = null) }
    }

    // ---------------------------------------------------------------- recording

    fun toggleRecording() {
        if (_state.value.recording) {
            // stopping is a flag, not a cancellation: a cancelled coroutine cannot hand
            // back the samples it spent thirty seconds collecting
            stopRequested = true
            return
        }
        stopRequested = false
        _state.update { it.copy(recording = true, elapsed = 0.0, level = 0f, error = null) }
        recordJob = viewModelScope.launch {
            runCatching {
                Audio.record(
                    shouldStop = { stopRequested },
                    onLevel = { lv -> _state.update { it.copy(level = lv) } },
                    onElapsed = { s -> _state.update { it.copy(elapsed = s) } },
                )
            }.onSuccess { rec ->
                captured = rec
                _state.update { it.copy(recording = false, level = 0f, hasTake = rec.seconds > 1.0) }
            }.onFailure { e ->
                // cancellation is how stopping is spelled, so it is not an error
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.update { it.copy(recording = false, error = errorFor(e, R.string.err_record_failed)) }
            }
        }
    }

    fun analyse() {
        val rec = captured ?: return
        // the count follows from the length alone, and settling it before the screen appears
        // keeps the Analysing text from changing under the reader
        val expected = Resampler.windowCount(rec.samples.size, rec.sampleRate)
        _state.update { it.copy(step = Step.ANALYSING, error = null, windows = expected) }
        analyseJob = viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.Default) {
                    val m = model ?: RaagIdentifier.load(getApplication()).also { model = it }
                    val sa = _state.value.saHz
                    if (kernel == null || kernelFor != sa) {
                        kernel = Cqt.Kernel.forTonic(sa)
                        kernelFor = sa
                    }
                    var out: RaagIdentifier.Analysis
                    val ms = measureTimeMillis {
                        out = m.analyse(rec.samples, rec.sampleRate, sa, kernel = kernel!!,
                            onProgress = { done, total ->
                                // the identify loop is plain blocking code, so cancellation
                                // only lands where it is checked -- once per window
                                coroutineContext.ensureActive()
                                Log.i(TAG, "window $done/$total")
                            })
                    }
                    Log.i(TAG, "identified ${rec.seconds}s in ${ms}ms, " +
                        "sa offset ${out.saOffsetCents?.let { "%.1f".format(it) } ?: "none"}")
                    out to ms
                }
            }.onSuccess { (out, ms) ->
                _state.update {
                    it.copy(
                        step = Step.RESULT, predictions = out.predictions, elapsedMillis = ms,
                        analysedSeconds = rec.seconds,
                        saDriftCents = out.saOffsetCents.takeIf { c -> SaCheck.isDrift(c) },
                    )
                }
            }.onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(TAG, "identify failed", e)
                _state.update {
                    it.copy(step = Step.RECORD, error = R.string.err_identify_failed)
                }
            }
        }
    }

    /** Take the tonic the recording actually used, and run the same take again. */
    fun rerunWithDetectedSa() {
        val drift = _state.value.saDriftCents ?: return
        val corrected = SaCheck.correctedTonic(_state.value.saHz, drift)
        Sa.remember(getApplication(), corrected)
        _state.update {
            it.copy(saHz = corrected, keyboardMidi = Sa.nearest(corrected).first,
                saDriftCents = null)
        }
        analyse()
    }

    fun cancelAnalysis() {
        analyseJob?.cancel()
        _state.update { it.copy(step = Step.RECORD) }
    }

    fun recordAgain() = _state.update {
        it.copy(step = Step.RECORD, predictions = emptyList(), elapsed = 0.0, hasTake = false)
    }

    fun changeSa() = _state.update { it.copy(step = Step.SET_SA, predictions = emptyList()) }

    fun back(): Boolean = when (_state.value.step) {
        Step.SET_SA -> false
        Step.RECORD -> { _state.update { it.copy(step = Step.SET_SA) }; true }
        Step.ANALYSING -> { cancelAnalysis(); true }
        Step.RESULT -> { recordAgain(); true }
    }

    /** A missing microphone is worth saying out loud; everything else gets the general line. */
    @StringRes
    private fun errorFor(e: Throwable, @StringRes fallback: Int): Int =
        if (e.message?.contains("microphone", ignoreCase = true) == true) R.string.err_no_mic
        else fallback

    companion object {
        const val MIN_SECONDS = 20.0
        private const val TAG = "mogra.identify"
    }
}
