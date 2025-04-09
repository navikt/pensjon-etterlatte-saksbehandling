package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.BrevFastInnholdData
import no.nav.etterlatte.brev.BrevRedigerbarInnholdData
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.pensjon.brevbaker.api.model.Kroner

object EtteroppgjoerBrevData {
    data class Forhaandsvarsel(
        val bosattUtland: Boolean,
        val norskInntekt: Boolean,
        val etteroppgjoersAar: Int,
        val rettsgebyrBeloep: Int,
        val resultatType: EtteroppgjoerResultatType,
        val inntekt: Kroner,
        val faktiskInntekt: Kroner,
        val avviksBeloep: Kroner,
    ) : BrevFastInnholdData() {
        override val type: String = "OMS_EO_FORHAANDSVARSEL"
        override val brevKode: Brevkoder = Brevkoder.OMS_EO_FORHAANDSVARSEL
    }

    data class ForhaandsvarselInnhold(
        val sak: Sak,
    ) : BrevRedigerbarInnholdData() {
        override val type: String = "OMS_EO_FORHAANDSVARSEL_REDIGERBAR"
        override val brevKode: Brevkoder = Brevkoder.OMS_EO_FORHAANDSVARSEL
    }
}
