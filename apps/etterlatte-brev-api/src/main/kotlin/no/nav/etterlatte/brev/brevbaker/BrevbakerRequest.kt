package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.adresse.Avsender
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.brevbaker.BrevbakerHelpers.mapFelles
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Verge
import no.nav.pensjon.brevbaker.api.model.Felles

data class BrevbakerRequest private constructor(
    val kode: EtterlatteBrevKode,
    val letterData: Any,
    val felles: Felles,
    val language: LanguageCode,
) {
    companion object {
        fun fra(
            brevKode: EtterlatteBrevKode,
            letterData: Any,
            avsender: Avsender,
            soekerOgEventuellVerge: SoekerOgEventuellVerge,
            sakId: Long,
            spraak: Spraak,
            sakType: SakType,
        ): BrevbakerRequest =
            BrevbakerRequest(
                kode = brevKode,
                letterData = letterData,
                felles =
                    mapFelles(
                        sakId = sakId,
                        soeker = soekerOgEventuellVerge.soeker,
                        avsender = avsender,
                        vergeNavn =
                            finnVergesNavn(
                                brevKode,
                                soekerOgEventuellVerge,
                                sakType,
                            ),
                    ),
                language = LanguageCode.spraakToLanguageCode(spraak),
            )

        private fun finnVergesNavn(
            brevKode: EtterlatteBrevKode,
            soekerOgEventuellVerge: SoekerOgEventuellVerge,
            sakType: SakType,
        ): String? {
            val harVerge = harVerge(soekerOgEventuellVerge, sakType)
            return if (erMigrering(brevKode) && harVerge) {
                soekerOgEventuellVerge.soeker.formaterNavn() + " ved verge"
            } else if (harVerge) {
                soekerOgEventuellVerge.verge?.navn()
                    ?: (soekerOgEventuellVerge.soeker.formaterNavn() + " ved verge")
            } else {
                null
            }
        }

        private fun harVerge(
            soekerOgEventuellVerge: SoekerOgEventuellVerge,
            sakType: SakType,
        ): Boolean {
            // Hvis under18 er true eller ukjent (null) sier vi at vi skal ha forelderverge i barnepensjonssaker
            val skalHaForelderVerge =
                sakType == SakType.BARNEPENSJON && soekerOgEventuellVerge.soeker.under18 != false
            return soekerOgEventuellVerge.verge != null || skalHaForelderVerge
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

enum class EtterlatteBrevKode(val tittel: String? = null) {
    BARNEPENSJON_AVSLAG,
    BARNEPENSJON_AVSLAG_UTFALL,
    BARNEPENSJON_INNVILGELSE,
    BARNEPENSJON_INNVILGELSE_UTFALL,
    BARNEPENSJON_OPPHOER,
    BARNEPENSJON_OPPHOER_UTFALL,
    BARNEPENSJON_REVURDERING,
    BARNEPENSJON_FORHAANDSVARSEL_OMREGNING,
    BARNEPENSJON_VEDTAK_OMREGNING,
    BARNEPENSJON_VEDTAK_OMREGNING_FERDIG,
    OMSTILLINGSSTOENAD_AVSLAG,
    OMSTILLINGSSTOENAD_AVSLAG_UTFALL,
    OMSTILLINGSSTOENAD_INNVILGELSE,
    OMSTILLINGSSTOENAD_INNVILGELSE_UTFALL,
    OMSTILLINGSSTOENAD_REVURDERING,
    OMSTILLINGSSTOENAD_REVURDERING_OPPHOER,
    OMSTILLINGSSTOENAD_REVURDERING_OPPHOER_UTFALL,
    OMSTILLINGSSTOENAD_REVURDERING_OPPHOER_MANUELL, // Denne bør fjernes
    TILBAKEKREVING_INNHOLD,
    TILBAKEKREVING_FERDIG,
    TOM_DELMAL,
    TOM_MAL_INFORMASJONSBREV,
    TOM_MAL,
    UTSATT_KLAGEFRIST("Informasjon om barnepensjon fra 1. januar 2024"),
    OMREGNING_INFORMASJON,
}

data class SoekerOgEventuellVerge(val soeker: Soeker, val verge: Verge?)
