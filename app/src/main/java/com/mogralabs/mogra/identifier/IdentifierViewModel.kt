package com.mogralabs.mogra.identifier

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mogralabs.mogra.audio.Cqt
import com.mogralabs.mogra.audio.RaagIdentifier
import com.mogralabs.mogra.audio.Yin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
        val error: String? = null,
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
    private var listenJob: Job? = null

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
        if (listenJob?.isActive == true) { listenJob?.cancel(); return }
        _state.update { it.copy(listening = true, heardHz = null, error = null) }
        listenJob = viewModelScope.launch {
            runCatching {
                val rec = Audio.record(maxSeconds = 5.0, onLevel = { lv ->
                    _state.update { it.copy(level = lv) }
                })
                withContext(Dispatchers.Default) { Yin.fromHum(rec.samples, rec.sampleRate) }
            }.onSuccess { hz ->
                _state.update {
                    it.copy(listening = false, heardHz = hz, saHz = hz,
                        keyboardMidi = Sa.nearest(hz).first)
                }
            }.onFailure { e ->
                _state.update { it.copy(listening = false, error = e.message ?: "could not hear a pitch") }
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
                _state.update { it.copy(recording = false, level = 0f) }
            }.onFailure { e ->
                // cancellation is how stopping is spelled, so it is not an error
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.update { it.copy(recording = false, error = e.message ?: "recording failed") }
            }
        }
    }

    fun analyse() {
        val rec = captured ?: return
        _state.update { it.copy(step = Step.ANALYSING, error = null, windows = 0) }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.Default) {
                    val m = model ?: RaagIdentifier.load(getApplication()).also { model = it }
                    val sa = _state.value.saHz
                    if (kernel == null || kernelFor != sa) {
                        kernel = Cqt.Kernel.forTonic(sa)
                        kernelFor = sa
                    }
                    var out: List<RaagIdentifier.Prediction>
                    val ms = measureTimeMillis {
                        out = m.predict(rec.samples, rec.sampleRate, sa, kernel = kernel!!,
                            onProgress = { done, total ->
                                _state.update { it.copy(windows = total) }
                                Log.i(TAG, "window $done/$total")
                            })
                    }
                    Log.i(TAG, "identified ${rec.seconds}s in ${ms}ms")
                    out to ms
                }
            }.onSuccess { (out, ms) ->
                _state.update {
                    it.copy(step = Step.RESULT, predictions = out, elapsedMillis = ms,
                        analysedSeconds = rec.seconds)
                }
            }.onFailure { e ->
                Log.e(TAG, "identify failed", e)
                _state.update {
                    it.copy(step = Step.RECORD, error = e.message ?: e::class.java.simpleName)
                }
            }
        }
    }

    fun cancelAnalysis() = _state.update { it.copy(step = Step.RECORD) }

    fun recordAgain() = _state.update {
        it.copy(step = Step.RECORD, predictions = emptyList(), elapsed = 0.0)
    }

    fun changeSa() = _state.update { it.copy(step = Step.SET_SA, predictions = emptyList()) }

    fun back(): Boolean = when (_state.value.step) {
        Step.SET_SA -> false
        Step.RECORD -> { _state.update { it.copy(step = Step.SET_SA) }; true }
        Step.ANALYSING -> { cancelAnalysis(); true }
        Step.RESULT -> { recordAgain(); true }
    }

    companion object {
        const val MIN_SECONDS = 20.0
        private const val TAG = "mogra.identify"
    }
}
