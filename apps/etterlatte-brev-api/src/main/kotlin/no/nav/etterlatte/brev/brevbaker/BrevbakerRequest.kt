package no.nav.etterlatte.brev.brevbaker

import no.nav.pensjon.brev.api.model.Felles

data class BrevbakerRequest(
    val kode: EtterlatteBrevKode,
    val letterData: Any,
    val felles: Felles,
    val language: LanguageCode
)

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

enum class EtterlatteBrevKode { A_LETTER, BARNEPENSJON_VEDTAK }