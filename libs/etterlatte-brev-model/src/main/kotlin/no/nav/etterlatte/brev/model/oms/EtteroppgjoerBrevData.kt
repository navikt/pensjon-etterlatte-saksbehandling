package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.BrevFastInnholdData
import no.nav.etterlatte.brev.BrevRedigerbarInnholdData
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.YearMonth

object EtteroppgjoerBrevData {
    data class Forhaandsvarsel(
        val bosattUtland: Boolean,
        val norskInntekt: Boolean,
        val etteroppgjoersAar: Int,
        val rettsgebyrBeloep: Kroner,
        val resultatType: EtteroppgjoerResultatType,
        val inntekt: Kroner,
        val faktiskInntekt: Kroner,
        val avviksBeloep: Kroner,
        val grunnlag: EtteroppgjoerBrevGrunnlag,
    ) : BrevFastInnholdData() {
        override val type: String = "OMS_EO_FORHAANDSVARSEL"
        override val brevKode: Brevkoder = Brevkoder.OMS_EO_FORHAANDSVARSEL
    }

    data class ForhaandsvarselInnhold(
        val sak: Sak,
        val bosattUtland: Boolean,
        val norskInntekt: Boolean,
        val etteroppgjoersAar: Int,
        val resultatType: EtteroppgjoerResultatType,
        val rettsgebyrBeloep: Kroner,
        val avviksBeloep: Kroner,
    ) : BrevRedigerbarInnholdData() {
        override val type: String = "OMS_EO_FORHAANDSVARSEL_REDIGERBAR"
        override val brevKode: Brevkoder = Brevkoder.OMS_EO_FORHAANDSVARSEL
    }

    data class Vedtak(
        val bosattUtland: Boolean,
    ) : BrevFastInnholdData() {
        override val type: String = "OMS_EO_VEDTAK"
        override val brevKode: Brevkoder = Brevkoder.OMS_EO_VEDTAK
    }

    class VedtakInnhold : BrevRedigerbarInnholdData() {
        override val type: String = "OMS_EO_VEDTAK_UTFALL"
        override val brevKode: Brevkoder = Brevkoder.OMS_EO_VEDTAK
    }
}

data class EtteroppgjoerBrevGrunnlag(
    val fom: YearMonth,
    val tom: YearMonth,
    val innvilgedeMaaneder: Int,
    val loennsinntekt: Kroner,
    val naeringsinntekt: Kroner,
    val afp: Kroner,
    val utlandsinntekt: Kroner,
)
