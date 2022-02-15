package no.nav.etterlatte.person.pdl

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.etterlatte.libs.common.pdl.ResponseError
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class FamilieRelasjonResponse(
    val data: FamilieRelasjonResponseData? = null,
    val errors: List<ResponseError>? = null
)

data class FamilieRelasjonResponseData(
    val hentPerson: HentFamilieRelasjon
)

data class HentFamilieRelasjon(
    val foreldreansvar: List<ForeldreAnsvar>,
    val forelderBarnRelasjon: List<ForelderBarnRelasjon>
)

data class ForeldreAnsvar(
    val ansvar: String? = null,
    val ansvarlig: String? = null,
    val ansvarligUtenIdentifikator: RelatertBiPerson? = null,
    val ansvarssubjekt: String? = null,
    val folkeregistermetadata: Folkeregistermetadata? = null,
    val metadata: Metadata,
)

data class RelatertBiPerson(
    val foedselsdato: LocalDate? = null,
    val kjoenn: String? = null,
    val navn: Personnavn? = null,
    val statsborgerskap: String? = null
)

data class Personnavn(
    val etternavn: String,
    val fornavn: String,
    val mellomnavn: String? = null
)

data class ForelderBarnRelasjon(
    val folkeregistermetadata: Folkeregistermetadata? = null,
    val metadata: Metadata,
    val minRolleForPerson: ForelderBarnRelasjonRolle? = null,
    val relatertPersonsIdent: String,
    val relatertPersonsRolle: ForelderBarnRelasjonRolle
)

enum class ForelderBarnRelasjonRolle {
    BARN,
    FAR,
    MEDMOR,
    MOR
}