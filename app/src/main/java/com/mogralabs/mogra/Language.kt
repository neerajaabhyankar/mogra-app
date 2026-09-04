package com.mogralabs.mogra

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Which language the app is in, and how it gets there.
 *
 * Done by hand rather than through `AppCompatDelegate.setApplicationLocales`, which would
 * mean pulling in appcompat for one feature in an app that is otherwise pure Compose. The
 * choice is a preference, the Activity wraps its base context with it, and changing it
 * recreates the Activity — which is what the framework does anyway.
 */
object Language {

    /** The three the app ships strings for. The tag is also the resource qualifier. */
    val TAGS = listOf("en", "mr", "hi")

    private const val PREFS = "mogra"
    private const val KEY = "language"

    /** The saved choice, or null when the user has never picked one. */
    fun saved(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)

    /**
     * The tag actually in force: the saved choice, else the best match for the system
     * language, else English.
     */
    fun current(context: Context): String {
        saved(context)?.let { return it }
        val system = context.resources.configuration.locales[0].language
        return if (system in TAGS) system else "en"
    }

    fun choose(context: Context, tag: String) {
        require(tag in TAGS) { "unknown language: $tag" }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, tag).apply()
    }

    /** A context whose resources resolve in the chosen language. */
    fun wrap(base: Context): Context {
        val locale = Locale.forLanguageTag(current(base))
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
