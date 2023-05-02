package no.nav.etterlatte.brev.brevbaker

import java.time.LocalDate

data class BrevbakerRequest(
    val kode: String,
    val letterData: Any,
    val felles: Felles,
    val language: LanguageCode
) {
    constructor(brevInfo: BrevInfo, language: LanguageCode) : this(
        kode = brevInfo.template,
        letterData = brevInfo.letterData,
        felles = brevInfo.felles,
        language = language
    )
}

enum class LanguageCode {
    BOKMAL, NYNORSK, ENGLISH;

    companion object {
        fun createLanguageCode(spraak: String): LanguageCode {
            return when (spraak.uppercase()) {
                ("EN") -> ENGLISH
                ("NB") -> BOKMAL
                ("NN") -> NYNORSK
                else -> standardLanguage()
            }
        }

        fun standardLanguage() = BOKMAL
    }
}

data class BrevInfo(val template: String, val letterData: Any, val felles: Felles)

data class Felles(
    val dokumentDato: LocalDate,
    val saksnummer: String,
    val avsenderEnhet: NAVEnhet? = null,
    val mottaker: Mottaker? = null,
    val signerendeSaksbehandlere: SignerendeSaksbehandlere? = null
)

data class Mottaker(
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String,
    val foedselsnummer: Foedselsnummer,
    val adresse: Adresse?
)

data class Adresse(
    val linje1: String,
    val linje2: String,
    val linje3: String? = null,
    val linje4: String? = null,
    val linje5: String? = null
)

data class SignerendeSaksbehandlere(val saksbehandler: String, val attesterendeSaksbehandler: String? = null)

data class NAVEnhet(
    val returAdresse: ReturAdresse,
    val nettside: String,
    val navn: String,
    val telefonnummer: Telefonnummer
)

data class ReturAdresse(val adresseLinje1: String, val postNr: String, val postSted: String)

data class Telefonnummer(val value: String)
data class Foedselsnummer(val value: String)