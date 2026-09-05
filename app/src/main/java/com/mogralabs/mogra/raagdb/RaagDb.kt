package com.mogralabs.mogra.raagdb

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * The tanarang raag database, as shipped in `assets/raagdb.json`.
 *
 * 116 raags with their aaroha, avaroha, mukhyanga, nyas swars, vaadi/samvaadi, thaat and
 * prahar — the same data behind tanarang.com, by way of libmogra, and generated into this
 * asset by `tools/gen_raagdb.py`.
 *
 * The by-swar index is an **exact** set match, which is what libmogra's own raagfinder does:
 * a raag's set is the swars of its aaroha and avaroha together, saptak marks dropped. So
 * asking for S R g m D finds the raags whose scale is precisely those five, not the ones
 * that merely contain them.
 */
class RaagDb private constructor(
    val swarOrder: List<String>,
    val raags: List<Raag>,
    private val bySwar: Map<String, List<Int>>,
) {

    class Raag(
        val key: String,
        val name: String,
        val swars: List<String>,
        /** Attribute name to its values; a multi-value attribute is mukhyanga's phrases. */
        val fields: Map<String, List<String>>,
    )

    /** Raags whose scale is exactly [swars], in the database's alphabetical order. */
    fun matching(swars: Collection<String>): List<Raag> {
        val key = swars.sortedBy { swarOrder.indexOf(it) }.joinToString(",")
        return bySwar[key].orEmpty().map { raags[it] }
    }

    fun byName(name: String): Raag? = raags.firstOrNull { it.name == name }

    companion object {
        @Volatile private var cached: RaagDb? = null

        /** Parsed once; 40 KB of JSON is not worth re-reading on every recomposition. */
        fun load(context: Context): RaagDb = cached ?: synchronized(this) {
            cached ?: parse(context.assets.open("raagdb.json").bufferedReader()
                .use { it.readText() }).also { cached = it }
        }

        fun parse(json: String): RaagDb {
            val root = JSONObject(json)
            val order = root.getJSONArray("swarOrder").strings()

            val array = root.getJSONArray("raags")
            val raags = (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                val fields = o.getJSONObject("fields")
                Raag(
                    key = o.getString("key"),
                    name = o.getString("name"),
                    swars = o.getJSONArray("swars").strings(),
                    fields = fields.keys().asSequence()
                        .associateWith { fields.getJSONArray(it).strings() },
                )
            }

            val index = root.getJSONObject("bySwar")
            val bySwar = index.keys().asSequence().associateWith { key ->
                index.getJSONArray(key).let { a -> (0 until a.length()).map { a.getInt(it) } }
            }
            return RaagDb(order, raags, bySwar)
        }

        private fun JSONArray.strings() = (0 until length()).map { getString(it) }
    }
}
