package person.pdl

import no.nav.etterlatte.libs.common.pdl.ResponseError
import java.util.*

data class UtlandResponse(
    val `data`: Data,
    val errors: List<ResponseError>? = null
)
data class Data(

    val hentPerson: HentPerson?
)

data class Statsborgerskap(
    val gyldigFraOgMed: String?,
    val gyldigTilOgMed: String?,
    val land: String,
    val metadata: Metadata
)

data class HentPerson(
    val innflyttingTilNorge: List<InnflyttingTilNorge>?,
    val statsborgerskap: List<Statsborgerskap>?,
    val utflyttingFraNorge: List<UtflyttingFraNorge>?
)

data class InnflyttingTilNorge(
    val folkeregistermetadata: Folkeregistermetadata?,
    val fraflyttingsland: String?,
    val fraflyttingsstedIUtlandet: String?,
    val metadata: Metadata
)
//bruke date?
data class UtflyttingFraNorge(
    val folkeregistermetadata: Folkeregistermetadata?,
    val tilflyttingsland: String?,
    val tilflyttingsstedIUtlandet: String?,
    val utflyttingsdato: String?,
    val metadata: Metadata
)

data class Folkeregistermetadata(
    val aarsak: String?,
    val ajourholdstidspunkt: String?,
    val gyldighetstidspunkt: String?,
    val kilde: String?,
    val opphoerstidspunkt: String?,
    val sekvens: String?
)

data class Endringer(
    val kilde: String?,
    val registrert: String?,
    val registrertAv: String?,
    val systemkilde: String?,
    val type: String?
)


data class Metadata(
    val endringer: List<Endringer>,
    val historisk: Boolean,
    val master: String,
    val opplysningsId: String?
)
