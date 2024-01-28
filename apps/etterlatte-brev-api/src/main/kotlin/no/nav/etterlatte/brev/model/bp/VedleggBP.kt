package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.model.BrevInnholdVedlegg
import no.nav.etterlatte.brev.model.BrevVedleggKey
import no.nav.etterlatte.brev.model.SlateHelper

class VedleggBP {
    companion object {
        fun innvilgelse() = listOf(beregningAvBarnepensjonTrygdetid())

        fun endring() = listOf(beregningAvBarnepensjonTrygdetid())

        private fun beregningAvBarnepensjonTrygdetid() =
            BrevInnholdVedlegg(
                tittel = "Trygdetid i vedlegg beregning av barnepensjon",
                key = BrevVedleggKey.BP_BEREGNING_TRYGDETID,
                payload = SlateHelper.getSlate("/maler/vedlegg/bp_beregning_trygdetid.json"),
            )
    }
}
