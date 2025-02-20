package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.FolkeregisteridentifikatorValidator
import java.time.LocalDate

/*
    innsender: Denne brukes til å indikere system eller saksbehandler
    ident(manuelt opprettet behandling) i tillegg til faktisk innsender(innbygger)
 */

data class Persongalleri(
    val soeker: Folkeregisteridentifikator,
    val innsender: Folkeregisteridentifikator? = null,
    val soesken: List<Folkeregisteridentifikator> = emptyList(),
    val avdoed: List<Folkeregisteridentifikator> = emptyList(),
    val gjenlevende: List<Folkeregisteridentifikator> = emptyList(),
    val personerUtenIdent: List<PersonUtenIdent>? = null,
    val annenForelder: AnnenForelder? = null,
) {
    override fun toString(): String =
        "Persongalleri(soeker=${soeker.value}," +
            "innsender=${innsender?.value}," +
            "soesken=${soesken.map { it.value }}," +
            "avdoed=${avdoed.map { it.value }}," +
            "gjenlevende=${gjenlevende.map { it.value }}," +
            "personerUtenIdent=${personerUtenIdent?.map { it.person.foedselsdato.toString() }})"

    fun validerFoedselesnummere(): Boolean =
        validateFnrSimple(soeker) &&
            validateFnrSimple(innsender) &&
            soesken.all { validateFnrSimple(it) } &&
            avdoed.all { validateFnrSimple(it) } &&
            gjenlevende.all { validateFnrSimple(it) }

    fun hentAlleIdentifikatorer(): List<Folkeregisteridentifikator> {
        val idents = mutableListOf<Folkeregisteridentifikator?>(soeker, innsender)
        idents.addAll(soesken)
        idents.addAll(avdoed)
        idents.addAll(gjenlevende)
        return idents.toList().mapNotNull { it }
    }
}

fun validateFnrSimple(fnr: Folkeregisteridentifikator?): Boolean {
    if (fnr == null) {
        return true
    }

    return FolkeregisteridentifikatorValidator.isValid(fnr.value)
}

enum class RelativPersonrolle {
    FORELDER,
    BARN,
}

data class PersonUtenIdent(
    val rolle: RelativPersonrolle,
    val person: RelatertPerson,
)

data class RelatertPerson(
    val foedselsdato: LocalDate? = null,
    val kjoenn: String? = null,
    val navn: Navn? = null,
    val statsborgerskap: String? = null,
)

data class RedigertFamilieforhold(
    val avdoede: List<String> = emptyList(),
    val gjenlevende: List<String> = emptyList(),
)

data class AnnenForelder(
    val vurdering: AnnenForelderVurdering,
    val begrunnelse: String? = null,
    val navn: String? = null,
    val foedselsdato: LocalDate? = null,
) {
    enum class AnnenForelderVurdering {
        KUN_EN_REGISTRERT_JURIDISK_FORELDER,
        FORELDER_UTEN_IDENT_I_PDL,
    }
}
