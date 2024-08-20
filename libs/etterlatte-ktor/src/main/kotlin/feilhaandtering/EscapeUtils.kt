package no.nav.etterlatte.libs.ktor.feilhaandtering

// Forenkla versjon av io.micrometer.core.instrument.util. StringEscapeUtils

object EscapeUtils {
    fun escape(str: String?) =
        str
            ?.map {
                replacements.getOrDefault(it, it).toString()
            }?.joinToString("") ?: ""

    private val replacements: Map<Char, String> =
        mapOf(
            '"' to "\\\"",
            '\\' to "\\\\",
            '\t' to "\\t",
            '\b' to "\\b",
            '\n' to "\\n",
            '\r' to "\\r",
            '\u000c' to "\\f", // \f, som er 13 i ascii-tabellen. Form feed, ikkje noko vi skal ha i praksis
        )
}
