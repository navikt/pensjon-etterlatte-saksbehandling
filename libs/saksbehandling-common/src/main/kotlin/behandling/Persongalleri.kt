package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.person.FolkeregisteridentifikatorValidator
import no.nav.etterlatte.libs.common.person.maskerFnr
import java.time.LocalDate

data class Persongalleri(
    val soeker: String,
    val innsender: String? = null,
    val soesken: List<String> = emptyList(),
    val avdoed: List<String> = emptyList(),
    val gjenlevende: List<String> = emptyList(),
    val personerUtenIdent: List<PersonUtenIdent>? = null,
    val annenForelder: AnnenForelder? = null,
) {
    override fun toString(): String =
        "Persongalleri(soeker=${soeker.maskerFnr()}," +
            "innsender=${innsender?.maskerFnr()}," +
            "soesken=${soesken.map { it.maskerFnr() }}," +
            "avdoed=${avdoed.map { it.maskerFnr() }}," +
            "gjenlevende=${gjenlevende.map { it.maskerFnr() }}," +
            "personerUtenIdent=${personerUtenIdent?.map { it.person.foedselsdato.toString() }})"

    fun validerFoedselesnummere(): Boolean =
        validateFnrSimple(soeker) &&
            validateFnrSimple(innsender) &&
            soesken.all { validateFnrSimple(it) } &&
            avdoed.all { validateFnrSimple(it) } &&
            gjenlevende.all { validateFnrSimple(it) }
}

fun validateFnrSimple(fnr: String?): Boolean {
    if (fnr == null) {
        return true
    }
    return FolkeregisteridentifikatorValidator.isValid(fnr)
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
    val begrunnelse: String?,
    val navn: String?,
    val foedselsdato: LocalDate?,
) {
    enum class AnnenForelderVurdering {
        KUN_EN_REGISTRERT_JURIDISK_FORELDER,
        FORELDER_UTEN_IDENT_I_PDL,
        AVDOED_FORELDER_UTEN_IDENT_I_PDL,
    }
}
