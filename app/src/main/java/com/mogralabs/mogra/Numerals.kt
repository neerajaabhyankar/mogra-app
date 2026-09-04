package com.mogralabs.mogra

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

/**
 * Digits in the script the language actually writes in: Devanagari for Marathi, Latin for
 * Hindi and English.
 *
 * Java's own locale data does this — `String.format(Locale("mr"), "%d", 4)` gives ४ on a
 * desktop JVM — but on the device it did not, and every formatted number came out Latin
 * while the surrounding Marathi text was right. Rather than chase which layer dropped the
 * locale, the numbers are converted here, where a unit test can hold it.
 *
 * Note names (`G♯3`), frequencies and the timer stay Latin in every language: they are
 * instrument readings rather than prose.
 */
object Numerals {

    private const val DEVANAGARI = "०१२३४५६७८९"

    /** Latin digits in [text] rewritten for [language]; everything else is left alone. */
    fun render(language: String, text: String): String {
        if (language != "mr") return text
        val out = StringBuilder(text.length)
        for (c in text) out.append(if (c in '0'..'9') DEVANAGARI[c - '0'] else c)
        return out.toString()
    }

    fun render(language: String, value: Int): String = render(language, value.toString())

    fun of(context: Context, value: Int): String =
        render(Language.current(context), value.toString())
}

/** The composable spelling of [Numerals.of]. */
@Composable
@ReadOnlyComposable
fun digits(value: Int): String = Numerals.of(LocalContext.current, value)
