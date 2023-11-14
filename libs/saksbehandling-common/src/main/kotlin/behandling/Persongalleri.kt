package no.nav.etterlatte.libs.common.behandling

import java.time.LocalDate

data class Persongalleri(
    val soeker: String,
    val innsender: String? = null,
    val soesken: List<String> = emptyList(),
    val avdoed: List<String> = emptyList(),
    val gjenlevende: List<String> = emptyList(),
    val personerUtenIdent: List<PersonUtenIdent>? = null,
)

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
