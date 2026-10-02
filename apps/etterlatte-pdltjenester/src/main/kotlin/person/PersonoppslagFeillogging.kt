package no.nav.etterlatte.person

import no.nav.etterlatte.sikkerLogg

internal suspend fun <T> personOppslag(
    operasjon: String,
    folkeregisteridentifikator: String,
    block: suspend () -> T,
): T = personOppslag(operasjon, listOf(folkeregisteridentifikator), block)

internal suspend fun <T> personOppslag(
    operasjon: String,
    folkeregisteridentifikatorer: List<String> = emptyList(),
    block: suspend () -> T,
): T =
    try {
        block()
    } catch (e: Exception) {
        val identifikatorer =
            folkeregisteridentifikatorer
                .takeIf { it.isNotEmpty() }
                ?.joinToString(prefix = " for folkeregisteridentifikator=", separator = ",")
                .orEmpty()
        sikkerLogg.error("$operasjon feilet $identifikatorer", e)
        throw e
    }
