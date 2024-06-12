package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.EtterlatteBrevKode
import no.nav.etterlatte.brev.adresse.Avsender
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.brevbaker.BrevbakerHelpers.mapFelles
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.notat.StrukturertBrev
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Verge
import no.nav.pensjon.brevbaker.api.model.Felles

data class BrevbakerRequest internal constructor(
    val kode: EtterlatteBrevKode,
    val letterData: Any,
    val felles: Felles,
    val language: LanguageCode,
) {
    companion object {
        fun fra(
            brevKode: EtterlatteBrevKode,
            brevData: BrevData,
            avsender: Avsender,
            soekerOgEventuellVerge: SoekerOgEventuellVerge,
            sakId: Long,
            spraak: Spraak,
            sakType: SakType,
        ): BrevbakerRequest =
            BrevbakerRequest(
                kode = brevKode,
                letterData = brevData,
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

        fun fraStrukturertBrev(
            strukturertBrev: StrukturertBrev,
            felles: Felles,
        ): BrevbakerRequest =
            BrevbakerRequest(
                kode = strukturertBrev.brevkode,
                letterData = strukturertBrev.tilLetterdata(),
                felles = felles,
                language = LanguageCode.spraakToLanguageCode(strukturertBrev.spraak),
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
        fun spraakToLanguageCode(spraak: Spraak): LanguageCode =
            when (spraak) {
                Spraak.EN -> ENGLISH
                Spraak.NB -> BOKMAL
                Spraak.NN -> NYNORSK
            }
    }
}

data class SoekerOgEventuellVerge(
    val soeker: Soeker,
    val verge: Verge?,
)
