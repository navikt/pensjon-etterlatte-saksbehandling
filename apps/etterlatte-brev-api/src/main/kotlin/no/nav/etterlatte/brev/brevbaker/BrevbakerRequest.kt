package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.adresse.Avsender
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.brevbaker.BrevbakerHelpers.mapFelles
import no.nav.etterlatte.brev.model.Spraak
import no.nav.pensjon.brevbaker.api.model.Felles

data class BrevbakerRequest(
    val kode: EtterlatteBrevKode,
    val letterData: Any,
    val felles: Felles,
    val language: LanguageCode,
) {
    companion object {
        fun fra(
            brevKode: EtterlatteBrevKode,
            letterData: Any,
            generellBrevData: GenerellBrevData,
            avsender: Avsender,
        ): BrevbakerRequest {
            return BrevbakerRequest(
                kode = brevKode,
                letterData = letterData,
                felles =
                    mapFelles(
                        sakId = generellBrevData.sak.id,
                        soeker = generellBrevData.personerISak.soeker,
                        avsender = avsender,
                        vergeNavn = generellBrevData.personerISak.verge?.navn,
                    ),
                language = LanguageCode.spraakToLanguageCode(generellBrevData.spraak),
            )
        }
    }
}

enum class LanguageCode {
    BOKMAL,
    NYNORSK,
    ENGLISH,
    ;

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
    BARNEPENSJON_AVSLAG_ENKEL,
    BARNEPENSJON_AVSLAG_IKKEYRKESSKADE,
    BARNEPENSJON_INNVILGELSE,
    BARNEPENSJON_INNVILGELSE_NY,
    BARNEPENSJON_INNVILGELSE_ENKEL,
    OMS_FOERSTEGANGSVEDTAK_INNVILGELSE,
    OMS_FOERSTEGANGSVEDTAK_INNVILGELSE_UTFALL,
    OMS_INNVILGELSE_MANUELL,
    OMS_AVSLAG,
    OMS_AVSLAG_BEGRUNNELSE,
    OMS_OPPHOER_MANUELL,
    OMS_REVURDERING_ENDRING,
    OMS_REVURDERING_OPPHOER,
    OMS_REVURDERING_OPPHOER_GENERELL,
    BARNEPENSJON_REVURDERING_ADOPSJON,
    BARNEPENSJON_REVURDERING_ENDRING,
    BARNEPENSJON_REVURDERING_OMGJOERING_AV_FARSKAP,
    BARNEPENSJON_REVURDERING_OPPHOER,
    BARNEPENSJON_REVURDERING_SOESKENJUSTERING,
    BARNEPENSJON_FORHAANDSVARSEL_OMREGNING,
    BARNEPENSJON_VEDTAK_OMREGNING,
    TILBAKEKREVING_INNHOLD,
    TILBAKEKREVING_FERDIG,
    TOM_MAL,
}
