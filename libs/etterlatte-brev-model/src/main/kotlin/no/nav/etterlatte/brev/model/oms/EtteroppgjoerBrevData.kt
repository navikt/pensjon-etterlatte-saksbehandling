package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.BrevFastInnholdData
import no.nav.etterlatte.brev.BrevInnholdVedlegg
import no.nav.etterlatte.brev.BrevRedigerbarInnholdData
import no.nav.etterlatte.brev.BrevVedleggInnholdData
import no.nav.etterlatte.brev.BrevVedleggKey
import no.nav.etterlatte.brev.BrevVedleggRedigerbarNy
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.Vedlegg
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.beregning.FaktiskInntektDto
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.YearMonth

object EtteroppgjoerBrevData {
    fun beregningsVedlegg(etteroppgjoersAar: Int): BrevVedleggRedigerbarNy =
        BrevVedleggRedigerbarNy(
            data = BeregningsVedleggInnhold(etteroppgjoersAar),
            vedlegg = Vedlegg.OMS_EO_FORHAANDSVARSEL_BEREGNINGVEDLEGG_INNHOLD,
            vedleggId = BrevVedleggKey.OMS_EO_FORHAANDSVARSEL_BEREGNING,
        )

    data class Forhaandsvarsel(
        val vedleggInnhold: List<Slate.Element> = emptyList(),
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

        // TODO: litt mer sjekker
        override fun medVedleggInnhold(innhold: () -> List<BrevInnholdVedlegg>): BrevFastInnholdData =
            this.copy(
                vedleggInnhold =
                    innhold()
                        .single {
                            it.key == BrevVedleggKey.OMS_EO_FORHAANDSVARSEL_BEREGNING
                        }.payload!!
                        .elements,
            )

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

    data class BeregningsVedleggInnhold(
        val etteroppgjoersAar: Int,
    ) : BrevVedleggInnholdData()

    data class Vedtak(
        val vedleggInnhold: List<Slate.Element> = emptyList(),
        val bosattUtland: Boolean,
        val etteroppgjoersAar: Int,
        val avviksBeloep: Kroner,
        val stoenadsBeloep: Kroner,
        val resultatType: EtteroppgjoerResultatType,
        val inntekt: Kroner,
        val faktiskInntekt: Kroner,
        val grunnlag: EtteroppgjoerBrevGrunnlag,
    ) : BrevFastInnholdData() {
        override val type: String = "OMS_EO_VEDTAK"

        override fun medVedleggInnhold(innhold: () -> List<BrevInnholdVedlegg>): BrevFastInnholdData =
            this.copy(
                vedleggInnhold =
                    innhold()
                        .single {
                            it.key == BrevVedleggKey.OMS_EO_FORHAANDSVARSEL_BEREGNING
                        }.payload!!
                        .elements,
            )

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
) {
    companion object {
        fun fra(grunnlag: FaktiskInntektDto) =
            EtteroppgjoerBrevGrunnlag(
                fom = grunnlag.fom,
                tom = grunnlag.tom!!,
                innvilgedeMaaneder = grunnlag.innvilgaMaaneder,
                loennsinntekt = Kroner(grunnlag.loennsinntekt),
                naeringsinntekt = Kroner(grunnlag.naeringsinntekt),
                afp = Kroner(grunnlag.afp),
                utlandsinntekt = Kroner(grunnlag.utlandsinntekt),
            )
    }
}
