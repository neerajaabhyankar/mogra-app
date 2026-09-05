package com.mogralabs.mogra.raagdb

/**
 * Writing swars in whatever script the app is currently in.
 *
 * The database stores the Latin shorthand every Indian theory book also uses — `S r R g G m
 * M P d D n N`, lower case for komal, capital M for teevra — with a leading `,` for the
 * mandra saptak and a backtick for taara. Displaying that in a Marathi or Hindi build means
 * translating the tokens, not the sentence.
 *
 * The Devanagari marks are the ones Unicode defines for Indian music: **U+0952 ANUDATTA**,
 * a line below the letter, for komal, and **U+0951 UDATTA**, a line above, for teevra Ma.
 * They sit correctly under रे and नि, which is where an underline or a borrowed halant would
 * have struggled. The saptak dots are U+0323 and U+0307.
 */
object Notation {

    /** The Latin shorthand, in the database's own order — S is 0, N is 11. */
    val LATIN = listOf("S", "r", "R", "g", "G", "m", "M", "P", "d", "D", "n", "N")

    /**
     * One swar of a phrase.
     *
     * [saptak] is -1 for mandra, 0 for madhya, +1 for taara. It is kept apart from [text]
     * because the mark is drawn rather than typed: the combining dots Unicode offers land on
     * top of the komal line below and the shirorekha above.
     */
    data class Token(val text: String, val swarIndex: Int, val saptak: Int)

    /** "S R g `S" -> four tokens, already spelled in [names]. */
    fun parse(raw: String, names: List<String>): List<Token> =
        raw.split(" ").filter { it.isNotBlank() }.mapNotNull { token ->
            val trimmed = token.trim()
            val index = LATIN.indexOf(trimmed.last().toString())
            if (index < 0) null else Token(
                text = names.getOrElse(index) { trimmed.last().toString() },
                swarIndex = index,
                saptak = when {
                    trimmed.startsWith(",") -> -1
                    trimmed.startsWith("`") -> 1
                    else -> 0
                },
            )
        }

    /**
     * One token — `S`, `g`, `` `S ``, `,D` — in the given script.
     *
     * [names] is the twelve swars in [LATIN] order, and [mandra]/[taara] are format strings
     * that place the saptak mark: `,%s` in Latin, `%s` plus a combining dot in Devanagari.
     */
    fun token(raw: String, names: List<String>, mandra: String, taara: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return trimmed
        val swar = trimmed.last().toString()
        val index = LATIN.indexOf(swar)
        val body = if (index >= 0) names.getOrElse(index) { swar } else return trimmed
        return when {
            trimmed.startsWith(",") -> mandra.format(body)
            trimmed.startsWith("`") -> taara.format(body)
            else -> body
        }
    }

    /** A whole phrase: "S R g m D `S" -> "सा रे ग॒ म ध सा̇". */
    fun phrase(raw: String, names: List<String>, mandra: String, taara: String): String =
        raw.split(" ").filter { it.isNotBlank() }
            .joinToString(" ") { token(it, names, mandra, taara) }
}
