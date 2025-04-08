package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.BrevFastInnholdData
import no.nav.etterlatte.brev.Brevkoder

object EtteroppgjoerBrevData {
    data class Forhaandsvarsel(
        val bosattUtland: Boolean,
        val norskInntekt: Boolean,
        val etteroppgjoersAar: Int,
        val rettsgebyrBeloep: Int,
        val resultatType: String,
        val inntekt: Int,
        val faktiskInntekt: Int,
        val avviksBeloep: Int,
    ) : BrevFastInnholdData() {
        override val type: String = "OMS_EO_FORHAANDSVARSEL"
        override val brevKode: Brevkoder = Brevkoder.OMS_EO_FORHAANDSVARSEL
    }
}
