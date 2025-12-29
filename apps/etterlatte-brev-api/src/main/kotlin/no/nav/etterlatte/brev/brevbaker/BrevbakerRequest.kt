package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.BrevData
import no.nav.etterlatte.brev.Brevbakerkode
import no.nav.etterlatte.brev.adresse.Avsender
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.brevbaker.BrevbakerHelpers.mapFelles
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Verge
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.pensjon.brevbaker.api.model.Felles

@ConsistentCopyVisibility
data class BrevbakerRequest internal constructor(
    val kode: Brevbakerkode,
    val letterData: Any,
    val felles: Felles,
    val language: LanguageCode,
) {
    companion object {
        fun fra(
            brevKode: Brevbakerkode,
            brevData: Any,
            avsender: Avsender,
            soekerOgEventuellVerge: SoekerOgEventuellVerge,
            sakId: SakId,
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
                        annenMottakerNavn =
                            finnVergesNavn(
                                soekerOgEventuellVerge,
                                sakType,
                            ),
                    ),
                language = LanguageCode.spraakToLanguageCode(spraak),
            )

        private fun finnVergesNavn(
            soekerOgEventuellVerge: SoekerOgEventuellVerge,
            sakType: SakType,
        ): String? =
            if (harVerge(soekerOgEventuellVerge, sakType)) {
                soekerOgEventuellVerge.soeker.formaterNavn() + " ved verge"
            } else {
                null
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
