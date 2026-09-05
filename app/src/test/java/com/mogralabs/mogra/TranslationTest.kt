package com.mogralabs.mogra

import com.mogralabs.mogra.Numerals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The three locales, checked against each other and against `translation.md`.
 *
 * A missing translation is invisible — Android silently falls back to English — so nothing
 * else in the build would tell us a string had been added to one file and not the others.
 */
class TranslationTest {

    private val res = File("src/main/res")
    private val table = File("../translation.md")

    private fun strings(dir: String): Map<String, String> {
        val text = File(res, "$dir/strings.xml").readText()
        return Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(text).associate { it.groupValues[1] to it.groupValues[2] }
    }

    private fun array(dir: String, name: String): List<String> {
        val text = File(res, "$dir/arrays.xml").readText()
        val block = Regex("""<string-array name="$name">(.*?)</string-array>""",
            RegexOption.DOT_MATCHES_ALL).find(text)!!.groupValues[1]
        return Regex("""<item>(.*?)</item>""").findAll(block).map { it.groupValues[1] }.toList()
    }

    /** Format specifiers, in order, so "%1$s · %2$d" and "%1$s · %2$s" do not both pass. */
    private fun specs(s: String) =
        Regex("""%(\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z]""").findAll(s).map { it.value }.toList()

    @Test fun `every locale has every string`() {
        val en = strings("values").keys
        listOf("values-mr", "values-hi").forEach { dir ->
            val other = strings(dir).keys
            assertEquals("$dir is missing: ${en - other}", emptySet<String>(), en - other)
            assertEquals("$dir has extra: ${other - en}", emptySet<String>(), other - en)
        }
    }

    @Test fun `format arguments match across locales`() {
        val en = strings("values")
        listOf("values-mr", "values-hi").forEach { dir ->
            strings(dir).forEach { (key, value) ->
                val want = specs(en.getValue(key))
                val got = specs(value)
                assertEquals("$dir/$key: \"$value\" against \"${en.getValue(key)}\"", want, got)
            }
        }
    }

    @Test fun `no locale left a string in English by accident`() {
        // Pure format strings are the same in all three languages by design -- the saptak
        // marks included, now that every language draws the dot rather than typing a comma.
        val sameEverywhere = setOf("percent_only", "step_of", "saptak_mandra", "saptak_taara")
        val noLetters = sameEverywhere
        val en = strings("values")
        listOf("values-mr", "values-hi").forEach { dir ->
            strings(dir).forEach { (key, value) ->
                if (key !in sameEverywhere) {
                    assertTrue("$dir/$key is byte-identical to English: \"$value\"",
                        value != en.getValue(key))
                }
                if (key !in noLetters) {
                    assertTrue("$dir/$key has no Devanagari: \"$value\"",
                        value.any { it in 'ऀ'..'ॿ' })
                }
            }
        }
    }

    @Test fun `raag names line up with the model's own list`() {
        val json = File("src/main/assets/model/raags.json").readText()
        val fromModel = Regex(""""([A-Za-z]+)"""").findAll(json).map { it.groupValues[1] }.toList()
        assertEquals(50, fromModel.size)
        listOf("values", "values-mr", "values-hi").forEach { dir ->
            assertEquals("$dir raag_names length", fromModel.size, array(dir, "raag_names").size)
        }
        // The English display names are the model's labels with the CamelCase opened up,
        // except where the common name differs from whatever the training set called it.
        // Checking against the model's order is what catches an accidental re-sort, which
        // would silently relabel every result.
        val renamed = mapOf(
            "KaushikDhwani" to "Kaushik Dhwani / Bhinna Shadja",
            "Sarang" to "Vrindavani Sarang",
            "Shivranjani" to "Shivaranjani",
        )
        array("values", "raag_names").forEachIndexed { i, shown ->
            val label = fromModel[i]
            val expected = renamed[label] ?: label.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
            assertEquals("raag_${"%02d".format(i)} (model label $label)", expected, shown)
        }
    }

    /**
     * The numeral policy. Marathi writes Devanagari digits, Hindi and English write Latin.
     *
     * This used to lean on Java's own locale data, which does the right thing on a desktop
     * JVM and did not on the phone — every formatted number came out Latin inside otherwise
     * correct Marathi. The conversion is [Numerals] now, and this is what holds it.
     */
    @Test fun `marathi renders devanagari digits and the others do not`() {
        assertEquals("२०", Numerals.render("mr", 20))
        assertEquals("४३%", Numerals.render("mr", "43%"))
        assertEquals("२ / २", Numerals.render("mr", "2 / 2"))
        assertEquals("20", Numerals.render("hi", 20))
        assertEquals("20", Numerals.render("en", 20))
    }

    /** Only digits change. Everything else — Devanagari, punctuation, ♯ — is untouched. */
    @Test fun `conversion leaves everything but digits alone`() {
        assertEquals("१ खंड · ५० राग", Numerals.render("mr", "1 खंड · 50 राग"))
        assertEquals("सा · +४ शतांश", Numerals.render("mr", "सा · +4 शतांश"))
    }

    /**
     * The numeric strings take %s, not %d, because the app formats the digits itself. A
     * %d here would silently go back to Latin on the device.
     */
    @Test fun `numeric strings use string placeholders`() {
        val numeric = listOf("peg_black", "peg_white", "cents_sharp", "cents_flat",
            "percent_only", "step_of", "result_footer", "cd_result_row")
        listOf("values", "values-mr", "values-hi").forEach { dir ->
            val all = strings(dir)
            numeric.forEach { key ->
                val value = all.getValue(key)
                assertTrue("$dir/$key still has a numeric placeholder: \"$value\"",
                    specs(value).none { it.endsWith("d") || it.endsWith("f") })
            }
        }
    }

    /**
     * The two arrays that index into shipped data. If the raag database grows and the table
     * does not, the app would show the wrong name against the right raag — worse than
     * showing nothing.
     */
    @Test fun `the database name and swar arrays match the data they index`() {
        val db = File("src/main/assets/raagdb.json").readText()
        val raagCount = Regex(""""key":"""").findAll(db).count()
        assertTrue("expected a populated raagdb.json", raagCount > 100)
        listOf("values", "values-mr", "values-hi").forEach { dir ->
            assertEquals("$dir db_raag_names", raagCount, array(dir, "db_raag_names").size)
            assertEquals("$dir swar_names", 12, array(dir, "swar_names").size)
        }
        // English keeps the shorthand the database itself is written in
        assertEquals(listOf("S", "r", "R", "g", "G", "m", "M", "P", "d", "D", "n", "N"),
            array("values", "swar_names"))
    }

    @Test fun `translation md covers every shipped string`() {
        val documented = Regex("""^\| `([a-z0-9_]+)` \|""", RegexOption.MULTILINE)
            .findAll(table.readText()).map { it.groupValues[1] }.toSet()
        val shipped = strings("values").keys - setOf("lang_en", "lang_mr", "lang_hi")
        assertEquals("strings not in translation.md: ${shipped - documented}",
            emptySet<String>(), shipped - documented)
    }
}
