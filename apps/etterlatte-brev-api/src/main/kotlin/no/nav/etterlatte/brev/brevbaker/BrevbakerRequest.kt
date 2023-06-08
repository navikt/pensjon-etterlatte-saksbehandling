package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.brevbaker.BrevbakerHelpers.hentBrevkode
import no.nav.etterlatte.brev.brevbaker.BrevbakerHelpers.mapFelles
import no.nav.etterlatte.brev.model.Avsender
import no.nav.etterlatte.brev.model.Spraak
import no.nav.pensjon.brevbaker.api.model.Felles

data class BrevbakerRequest(
    val kode: EtterlatteBrevKode,
    val letterData: Any,
    val felles: Felles,
    val language: LanguageCode
) {
    companion object {
        fun fra(
            letterData: Any,
            behandling: Behandling,
            avsender: Avsender
        ): BrevbakerRequest {
            return BrevbakerRequest(
                kode = hentBrevkode(
                    behandling.sakType,
                    behandling.vedtak.type
                ),
                letterData = letterData,
                felles = mapFelles(
                    sakId = behandling.sakId,
                    soeker = behandling.persongalleri.soeker,
                    avsender = avsender
                ),
                language = LanguageCode.spraakToLanguageCode(behandling.spraak)
            )
        }
    }
}

enum class LanguageCode {
    BOKMAL, NYNORSK, ENGLISH;

    companion object {
        fun spraakToLanguageCode(spraak: Spraak): LanguageCode {
            return when (spraak) {
                Spraak.EN -> ENGLISH
                Spraak.NB -> BOKMAL
                Spraak.NN -> NYNORSK
            }
        }
    }
}

enum class EtterlatteBrevKode {
    BARNEPENSJON_INNVILGELSE,
    OMS_INNVILGELSE_MANUELL
}