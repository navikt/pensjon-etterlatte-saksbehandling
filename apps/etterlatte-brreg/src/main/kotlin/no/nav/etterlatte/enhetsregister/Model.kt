package no.nav.etterlatte.enhetsregister

data class Organisasjonsform(
    val kode: String,
    val beskrivelse: String
)

data class Enhet(
    val organisasjonsnummer: String,
    val navn: String,
    val organisasjonsform: Organisasjonsform
)

data class Feilmelding(
    val status: Int,
    val feilmelding: String?,
    val valideringsfeil: List<Feil>
) {
    data class Feil(
        val feilmelding: String?,
        val parametere: List<String>,
        val feilaktigVerdi: String?
    )
}

data class BrregResponse(
    val resultat: Any? = null,
    val feilmelding: Feilmelding? = null
)