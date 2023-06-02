package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.brevbaker.BrevbakerHelpers.mapFelles
import no.nav.etterlatte.brev.model.Avsender
import no.nav.etterlatte.brev.model.BrevDataMapper
import no.nav.etterlatte.brev.model.Mottaker
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
            behandling: Behandling,
            avsender: Avsender,
            mottaker: Mottaker
        ): BrevbakerRequest {
            return BrevbakerRequest(
                kode = EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE, // TODO: Sett opp støtte for OMS - EY-1774
                letterData = BrevDataMapper.fra(behandling, avsender, mottaker),
                felles = mapFelles(behandling, avsender, mottaker),
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
    A_LETTER,
    BARNEPENSJON_INNVILGELSE,
    OMS_INNVILGELSE_MANUELL
}