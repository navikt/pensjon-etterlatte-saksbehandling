package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.BrevInnholdData
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.libs.common.sak.Sak

sealed class EtteroppgjoerBrevData : BrevInnholdData() {
    override val type: String = "ETTEROPPGJOER"
    abstract val sak: Sak

    data class VarselTilbakekreving(
        override val sak: Sak,
    ) : EtteroppgjoerBrevData() {
        override val brevKode: Brevkoder = Brevkoder.OMS_EO_VARSEL_TILBAKEKREVING
    }
}
