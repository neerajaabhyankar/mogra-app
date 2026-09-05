package com.mogralabs.mogra.raagdb

import kotlin.math.pow

/**
 * Turning a swar into a frequency, given Sa.
 *
 * Equal temperament for now: every swar is a whole number of semitones above Sa. That is
 * wrong in the way every keyboard is wrong — a Hindustani raag's komal Ga is not the
 * piano's, and two raags sharing a swar name do not always share its pitch.
 *
 * This is deliberately the only place that assumption lives. When per-raag shruti data
 * arrives, [cents] becomes a lookup keyed on the raag rather than a multiplication, and
 * nothing above it has to change.
 */
object Tuning {

    /** Semitones above Sa for each swar, in [Notation.LATIN] order. */
    private val SEMITONES = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)

    /** Cents above Sa. The seam: a raag-aware tuning replaces the arithmetic here. */
    fun cents(swarIndex: Int, saptak: Int = 0): Double =
        (SEMITONES[swarIndex] + 12 * saptak) * 100.0

    fun frequency(saHz: Double, swarIndex: Int, saptak: Int = 0): Double =
        saHz * 2.0.pow(cents(swarIndex, saptak) / 1200.0)

    /** A phrase of tokens as frequencies, in order. */
    fun frequencies(saHz: Double, tokens: List<Notation.Token>): List<Double> =
        tokens.map { frequency(saHz, it.swarIndex, it.saptak) }
}
