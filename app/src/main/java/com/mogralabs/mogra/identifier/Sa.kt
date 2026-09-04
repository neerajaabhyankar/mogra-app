package com.mogralabs.mogra.identifier

import android.content.Context
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Sa as the app talks about it: a frequency, the note name nearest to it, and where on the
 * keyboard it sits.
 *
 * The keyboard runs A2 to E4 — low enough for a male Sa around C3–E3, high enough for a
 * soprano Sa above C4, with one octave visible at a time and the rest reached by dragging.
 */
object Sa {

    /** MIDI note numbers for the ends of the keyboard. A2 = 45, E4 = 64. */
    const val LOWEST_MIDI = 45
    const val HIGHEST_MIDI = 64
    const val DEFAULT_MIDI = 49                     // C♯3, 138.59 Hz

    private const val PREFS = "mogra"
    private const val KEY_SA_HZ = "sa_hz"

    private val NAMES = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")

    /** True for the five black keys of each octave. */
    fun isAccidental(midi: Int): Boolean = NAMES[((midi % 12) + 12) % 12].length > 1

    fun hzOf(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)

    fun nameOf(midi: Int): String = NAMES[((midi % 12) + 12) % 12]

    /** "C♯3" — the name with its octave, the way the whole flow refers to Sa. */
    fun fullNameOf(midi: Int): String = nameOf(midi) + (midi / 12 - 1)

    /** The nearest semitone to a frequency, and how far off it is in cents. */
    fun nearest(hz: Double): Pair<Int, Double> {
        val exact = 69.0 + 12.0 * (ln(hz / 440.0) / ln(2.0))
        val midi = exact.roundToInt()
        return midi to (exact - midi) * 100.0
    }

    /** "C♯3 +4 cents", or just "C♯3" when it is within a cent. */
    fun describe(hz: Double): String {
        val (midi, cents) = nearest(hz)
        val name = fullNameOf(midi)
        return if (abs(cents) < 1.0) name
        else "$name ${if (cents > 0) "+" else "−"}${abs(cents).roundToInt()} cents"
    }

    /**
     * Kali 1 to Kali 5, Safed 1 to Safed 7 — the harmonium-peg names most people actually
     * use for their Sa. Null outside the range they cover.
     */
    fun pegName(midi: Int): String? {
        val white = intArrayOf(0, 2, 4, 5, 7, 9, 11)
        val pc = ((midi % 12) + 12) % 12
        val whiteIndex = white.indexOf(pc)
        if (whiteIndex >= 0) return "Safed ${whiteIndex + 1}"
        val blackOrder = intArrayOf(1, 3, 6, 8, 10)
        val blackIndex = blackOrder.indexOf(pc)
        return if (blackIndex >= 0) "Kali ${blackIndex + 1}" else null
    }

    /** The last Sa the user set, or C♯3 the first time. Kept so the flow never asks twice. */
    fun remembered(context: Context): Double =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getFloat(KEY_SA_HZ, hzOf(DEFAULT_MIDI).toFloat()).toDouble()

    fun remember(context: Context, hz: Double) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putFloat(KEY_SA_HZ, hz.toFloat()).apply()
    }
}
