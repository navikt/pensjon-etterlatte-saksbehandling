package person.pdl

data class Utland(
    val `data`: Data
)
data class Data(
    val hentPerson: HentPerson
)

data class Statsborgerskap(
    val gyldigFraOgMed: String,
    val gyldigTilOgMed: Any,
    val land: String,
    val metadata: MetadataX
)

data class HentPerson(
    val innflyttingTilNorge: List<InnflyttingTilNorge>,
    val statsborgerskap: List<Statsborgerskap>,
    val utflyttingFraNorge: List<UtflyttingFraNorge>
)

data class InnflyttingTilNorge(
    val folkeregistermetadata: Folkeregistermetadata,
    val fraflyttingsland: String,
    val fraflyttingsstedIUtlandet: Any,
    val metadata: Metadata
)

data class UtflyttingFraNorge(
    val folkeregistermetadata: Folkeregistermetadata,
    val tilflyttingsland: String,
    val tilflyttingsstedIUtlandet: Any,
    val utflyttingsdato: Any,
    val metadata: Metadata
)

data class Folkeregistermetadata(
    val aarsak: Any,
    val ajourholdstidspunkt: String,
    val gyldighetstidspunkt: String,
    val kilde: String,
    val opphoerstidspunkt: Any,
    val sekvens: Any
)

data class Endringer(
    val kilde: String,
    val registrert: String,
    val registrertAv: String,
    val systemkilde: String,
    val type: String
)

data class EndringerX(
    val registrert: String,
    val type: String
)

data class Metadata(
    val endringer: List<Endringer>,
    val historisk: Boolean,
    val master: String,
    val opplysningsId: String
)

data class MetadataX(
    val endringer: List<EndringerX>,
    val master: String,
    val opplysningsId: String
)