package no.nav.etterlatte.brev.model.tilbakkreving

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.libs.common.behandling.SakType

data class TilbakekrevingInnholdData(
    val harRenter: Boolean,
) : BrevData() {
    companion object {
        fun fra(generellBrevData: GenerellBrevData) =
            TilbakekrevingInnholdData(
                harRenter = true, // TODO EY-2806
            )
    }
}

data class TilbakekrevingFerdigData(
    val innhold: List<Slate.Element>,
    val erOMS: Boolean,
    val erBP: Boolean,
) : BrevData() {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            innholdMedVedlegg: InnholdMedVedlegg,
        ) = TilbakekrevingFerdigData(
            innhold = innholdMedVedlegg.innhold(),
            erOMS = generellBrevData.sak.sakType == SakType.OMSTILLINGSSTOENAD,
            erBP = generellBrevData.sak.sakType == SakType.BARNEPENSJON,
        )
    }
}
