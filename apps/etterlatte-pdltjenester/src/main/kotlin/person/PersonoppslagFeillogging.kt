package no.nav.etterlatte.person

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonIdent
import no.nav.etterlatte.sikkerLogg

internal fun sikkerloggOgKast(
    operasjon: String,
    folkeregisteridentifikator: Folkeregisteridentifikator,
    e: Exception,
): Nothing = sikkerloggOgKast(operasjon, listOf(PersonIdent(folkeregisteridentifikator.value)), e)

internal fun sikkerloggOgKast(
    operasjon: String,
    personIdent: PersonIdent,
    e: Exception,
): Nothing {
    sikkerloggOgKast(operasjon, listOf(personIdent), e)
}

internal fun sikkerloggOgKast(
    operasjon: String,
    folkeregisteridentifikatorer: List<PersonIdent> = emptyList(),
    e: Exception,
): Nothing {
    val identifikatorerUmaskert =
        folkeregisteridentifikatorer
            .takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = " for folkeregisteridentifikator=", separator = ",") { it.value }
            .orEmpty()

    sikkerLogg.error("$operasjon feilet$identifikatorerUmaskert", e)
    throw e
}
