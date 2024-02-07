package no.nav.etterlatte.brev.model.klage

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.brev.model.InnholdMedVedlegg
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.libs.common.behandling.SakType

data class AvvistKlageBrevData(val innhold: List<Slate.Element>, val data: AvvistKlageInnholdBrevData) : BrevData() {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            innholdMedVedlegg: InnholdMedVedlegg,
        ): AvvistKlageBrevData {
            return AvvistKlageBrevData(
                innhold = innholdMedVedlegg.innhold(),
                data = AvvistKlageInnholdBrevData.fra(generellBrevData),
            )
        }
    }
}

// TODO: Mer innhold inn i greia
data class AvvistKlageInnholdBrevData(
    val sakType: SakType,
) : BrevData() {
    companion object {
        fun fra(generellBrevData: GenerellBrevData): AvvistKlageInnholdBrevData {
            val klage = generellBrevData.forenkletVedtak?.klage ?: throw IllegalArgumentException("Vedtak mangler klage")
            return AvvistKlageInnholdBrevData(
                sakType = klage.sak.sakType,
            )
        }
    }
}
