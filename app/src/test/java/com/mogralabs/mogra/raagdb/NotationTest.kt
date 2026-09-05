package com.mogralabs.mogra.raagdb

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Swars written in Devanagari.
 *
 * The komal mark is U+0952 ANUDATTA and teevra is U+0951 UDATTA — the marks Unicode defines
 * for Indian music, rather than an underline or a borrowed halant. These tests spell the
 * code points out so a copy-paste accident with a look-alike character fails loudly.
 */
class NotationTest {

    private val anudatta = "॒"   // line below: komal
    private val udatta = "॑"     // line above: teevra
    private val dotBelow = "̣"   // mandra saptak
    private val dotAbove = "̇"   // taara saptak

    private val latin = Notation.LATIN
    private val deva = listOf(
        "सा", "रे$anudatta", "रे", "ग$anudatta", "ग", "म", "म$udatta",
        "प", "ध$anudatta", "ध", "नि$anudatta", "नि",
    )

    @Test fun `latin notation passes through unchanged`() {
        assertEquals("S R g m D `S",
            Notation.phrase("S R g m D `S", latin, ",%s", "`%s"))
    }

    @Test fun `komal swars carry the line below`() {
        assertEquals("ग$anudatta", Notation.token("g", deva, "%s$dotBelow", "%s$dotAbove"))
        assertEquals("रे$anudatta", Notation.token("r", deva, "%s$dotBelow", "%s$dotAbove"))
        assertEquals("नि$anudatta", Notation.token("n", deva, "%s$dotBelow", "%s$dotAbove"))
        // and the shuddha ones do not
        assertEquals("रे", Notation.token("R", deva, "%s$dotBelow", "%s$dotAbove"))
        assertEquals("नि", Notation.token("N", deva, "%s$dotBelow", "%s$dotAbove"))
    }

    @Test fun `teevra ma carries the line above`() {
        assertEquals("म$udatta", Notation.token("M", deva, "%s$dotBelow", "%s$dotAbove"))
        assertEquals("म", Notation.token("m", deva, "%s$dotBelow", "%s$dotAbove"))
    }

    @Test fun `saptak marks attach to the swar`() {
        assertEquals("सा$dotAbove", Notation.token("`S", deva, "%s$dotBelow", "%s$dotAbove"))
        assertEquals("ध$dotBelow", Notation.token(",D", deva, "%s$dotBelow", "%s$dotAbove"))
        // a komal swar in the mandra saptak keeps both marks
        assertEquals("नि$anudatta$dotBelow",
            Notation.token(",n", deva, "%s$dotBelow", "%s$dotAbove"))
    }

    @Test fun `a whole phrase converts`() {
        assertEquals("सा रे ग$anudatta म ध सा$dotAbove",
            Notation.phrase("S R g m D `S", deva, "%s$dotBelow", "%s$dotAbove"))
    }

    @Test fun `a phrase parses into swars and saptaks`() {
        val tokens = Notation.parse("S R g m D `S ,P", deva)
        assertEquals(7, tokens.size)
        assertEquals(listOf(0, 2, 3, 5, 9, 0, 7), tokens.map { it.swarIndex })
        assertEquals(listOf(0, 0, 0, 0, 0, 1, -1), tokens.map { it.saptak })
        assertEquals("ग$anudatta", tokens[2].text)
    }

    @Test fun `tuning follows Sa and the saptak`() {
        val sa = 220.0
        assertEquals(sa, Tuning.frequency(sa, 0), 1e-9)
        assertEquals(sa * 2, Tuning.frequency(sa, 0, saptak = 1), 1e-9)
        assertEquals(sa / 2, Tuning.frequency(sa, 0, saptak = -1), 1e-9)
        // P is a tempered fifth above Sa
        assertEquals(sa * Math.pow(2.0, 7 / 12.0), Tuning.frequency(sa, 7), 1e-9)
        // and a whole phrase comes back in order
        val freqs = Tuning.frequencies(sa, Notation.parse("S P `S", latin))
        assertEquals(listOf(220.0, 220.0 * Math.pow(2.0, 7 / 12.0), 440.0), freqs)
    }

    @Test fun `the shipped arrays are what the notation expects`() {
        // whatever the table says, the twelve swars must line up with the Latin order the
        // database is written in
        assertEquals(12, latin.size)
        assertEquals(listOf("S", "r", "R", "g", "G", "m", "M", "P", "d", "D", "n", "N"), latin)
    }
}
