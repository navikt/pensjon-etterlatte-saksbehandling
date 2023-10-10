package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.SlateHelper

class VedleggOMS {
    companion object {
        fun inntektsendringOMS(): List<BrevInnholdVedlegg> =
            listOf(
                utfallBeregningOMS(),
            )

        fun innvilgelseOMS(): List<BrevInnholdVedlegg> =
            listOf(
                utfallBeregningOMS(),
            )

        private fun utfallBeregningOMS() =
            BrevInnholdVedlegg(
                tittel = "Utfall ved beregning av omstillingsstønad",
                key = BrevVedleggKey.BEREGNING_INNHOLD,
                payload = SlateHelper.getSlate("/maler/vedlegg/oms_utfall_beregning.json"),
            )
    }
}
