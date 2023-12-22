package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.adresse.Avsender
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.brevbaker.BrevbakerHelpers.mapFelles
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.behandling.SakType
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
            // Hvis under18 er true eller ukjent (null) sier vi at vi skal ha forelderverge i barnepensjonssaker
            val skalHaForelderVerge =
                generellBrevData.sak.sakType == SakType.BARNEPENSJON && generellBrevData.personerISak.soeker.under18 != false
            val harVerge = generellBrevData.personerISak.verge != null || skalHaForelderVerge

            return BrevbakerRequest(
                kode = brevKode,
                letterData = letterData,
                felles =
                    mapFelles(
                        sakId = generellBrevData.sak.id,
                        soeker = generellBrevData.personerISak.soeker,
                        avsender = avsender,
                        vergeNavn =
                            if (erMigrering(brevKode) && harVerge) {
                                generellBrevData.personerISak.soeker.formaterNavn() + " ved verge"
                            } else if (harVerge) {
                                generellBrevData.personerISak.verge?.navn()
                                    ?: (generellBrevData.personerISak.soeker.formaterNavn() + " ved verge")
                            } else {
                                null
                            },
                    ),
                language = LanguageCode.spraakToLanguageCode(generellBrevData.spraak),
            )
        }

        private fun erMigrering(brevKode: EtterlatteBrevKode): Boolean =
            brevKode in
                listOf(
                    EtterlatteBrevKode.BARNEPENSJON_FORHAANDSVARSEL_OMREGNING,
                    EtterlatteBrevKode.BARNEPENSJON_VEDTAK_OMREGNING,
                    EtterlatteBrevKode.BARNEPENSJON_VEDTAK_OMREGNING_FERDIG,
                )
    }
}

fun Soeker.formaterNavn() = listOfNotNull(fornavn, mellomnavn, etternavn).joinToString(" ")

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
    BARNEPENSJON_INNVILGELSE,
    BARNEPENSJON_INNVILGELSE_NY,
    BARNEPENSJON_INNVILGELSE_ENKEL,
    OMS_FOERSTEGANGSVEDTAK_INNVILGELSE,
    OMS_FOERSTEGANGSVEDTAK_INNVILGELSE_UTFALL,
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
    BARNEPENSJON_VEDTAK_OMREGNING_FERDIG,
    TILBAKEKREVING_INNHOLD,
    TILBAKEKREVING_FERDIG,
    TOM_MAL_INFORMASJONSBREV,
    TOM_MAL,
}
