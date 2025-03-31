package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.BrevDataRedigerbar
import no.nav.etterlatte.brev.BrevFastInnholdData
import no.nav.etterlatte.brev.BrevRedigerbarInnholdData
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.libs.common.sak.Sak

object EtteroppgjoerBrevData {
    data class VarselTilbakekrevingInnhold(
        val sak: Sak,
    ) : BrevRedigerbarInnholdData(),
        BrevDataRedigerbar {
        override val type: String = "EO_VARSEL_TILBAKEKREVING_REDIGERBAR"
        override val brevKode: Brevkoder = Brevkoder.OMS_EO_VARSEL_TILBAKEKREVING
    }

    data class VarselTilbakekreving(
        val sak: Sak,
    ) : BrevFastInnholdData() {
        override val type: String = "EO_VARSEL_TILBAKEKREVING"
        override val brevKode: Brevkoder = Brevkoder.OMS_EO_VARSEL_TILBAKEKREVING
    }
}
