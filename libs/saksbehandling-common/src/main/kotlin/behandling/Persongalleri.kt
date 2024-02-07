package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.person.maskerFnr
import java.time.LocalDate

data class Persongalleri(
    val soeker: String,
    val innsender: String? = null,
    val soesken: List<String> = emptyList(),
    val avdoed: List<String> = emptyList(),
    val gjenlevende: List<String> = emptyList(),
    val personerUtenIdent: List<PersonUtenIdent>? = null,
) {
    override fun toString(): String {
        return "Persongalleri(soeker=${soeker.maskerFnr()}," +
            "innsender=${innsender?.maskerFnr()}," +
            "soesken=${soesken.map { it.maskerFnr() }}," +
            "avdoed=${avdoed.map { it.maskerFnr() }}," +
            "gjenlevende=${gjenlevende.map { it.maskerFnr() }}," +
            "personerUtenIdent=${personerUtenIdent?.map { it.person.foedselsdato.toString() }})"
    }
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
