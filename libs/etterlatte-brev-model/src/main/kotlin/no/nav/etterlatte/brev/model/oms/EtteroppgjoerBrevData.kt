package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.brev.BrevFastInnholdData
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.libs.common.sak.Sak

sealed class EtteroppgjoerBrevData : BrevFastInnholdData() {
    abstract val sak: Sak

    data class VarselTilbakekrevingInnhold(
        override val sak: Sak,
    ) : EtteroppgjoerBrevData(),
        BrevDataRedigerbar {
        override val type: String = "EO_VARSEL_TILBAKEKREVING_REDIGERBAR"
        override val brevKode: Brevkoder = Brevkoder.OMS_EO_VARSEL_TILBAKEKREVING
    }

    data class VarselTilbakekreving(
        override val sak: Sak,
    ) : EtteroppgjoerBrevData(),
        Brev {
        override val type: String = "EO_VARSEL_TILBAKEKREVING"
        override val brevKode: Brevkoder = Brevkoder.OMS_EO_VARSEL_TILBAKEKREVING
    }
}
