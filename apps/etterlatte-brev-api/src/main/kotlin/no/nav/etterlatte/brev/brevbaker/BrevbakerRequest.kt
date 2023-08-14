package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.behandling.Behandling
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
            brevKode: EtterlatteBrevKode,
            letterData: Any,
            behandling: Behandling,
            avsender: Avsender
        ): BrevbakerRequest {
            return BrevbakerRequest(
                kode = brevKode,
                letterData = letterData,
                felles = mapFelles(
                    sakId = behandling.sakId,
                    soeker = behandling.persongalleri.soeker,
                    avsender = avsender,
                    vergeNavn = behandling.persongalleri.verge?.navn
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
    BARNEPENSJON_AVSLAG,
    BARNEPENSJON_AVSLAG_IKKEYRKESSKADE,
    BARNEPENSJON_INNVILGELSE,
    BARNEPENSJON_INNVILGELSE_NY,
    OMS_INNVILGELSE_AUTO,
    OMS_INNVILGELSE_MANUELL,
    OMS_OPPHOER_MANUELL,
    BARNEPENSJON_REVURDERING_ADOPSJON,
    BARNEPENSJON_REVURDERING_ENDRING,
    BARNEPENSJON_REVURDERING_FENGSELSOPPHOLD,
    BARNEPENSJON_REVURDERING_HAR_STANSET,
    BARNEPENSJON_REVURDERING_OMGJOERING_AV_FARSKAP,
    BARNEPENSJON_REVURDERING_OPPHOER,
    BARNEPENSJON_REVURDERING_SOESKENJUSTERING,
    BARNEPENSJON_REVURDERING_YRKESSKADE,
    BARNEPENSJON_REVURDERING_UT_AV_FENGSEL
}