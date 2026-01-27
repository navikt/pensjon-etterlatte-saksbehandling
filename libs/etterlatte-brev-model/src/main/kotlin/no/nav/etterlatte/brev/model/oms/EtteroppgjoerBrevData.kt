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
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate
import java.time.YearMonth

object EtteroppgjoerBrevData {
    fun beregningsVedlegg(
        etteroppgjoersAar: Int,
        erVedtak: Boolean,
    ): BrevVedleggRedigerbarNy =
        BrevVedleggRedigerbarNy(
            data = BeregningsVedleggInnhold(etteroppgjoersAar, erVedtak),
            vedlegg = Vedlegg.OMS_EO_BEREGNINGVEDLEGG_INNHOLD,
            vedleggId = BrevVedleggKey.OMS_EO_BEREGNINGSVEDLEGG,
        )

    data class Forhaandsvarsel(
        val vedleggInnhold: List<Slate.Element> = emptyList(),
        val bosattUtland: Boolean,
        val norskInntekt: Boolean,
        val etteroppgjoersAar: Int,
        val rettsgebyrBeloep: Kroner,
        val resultatType: EtteroppgjoerResultatType,
        val stoenad: Kroner,
        val faktiskStoenad: Kroner,
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
                            it.key == BrevVedleggKey.OMS_EO_BEREGNINGSVEDLEGG
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
        val erVedtak: Boolean,
    ) : BrevVedleggInnholdData() {
        override val type: String = "OMS_EO_BEREGNINGVEDLEGG_INNHOLD"
        override val brevKode: Vedlegg = Vedlegg.OMS_EO_BEREGNINGVEDLEGG_INNHOLD
    }

    data class Vedtak(
        val vedleggInnhold: List<Slate.Element> = emptyList(),
        val bosattUtland: Boolean,
        val etteroppgjoersAar: Int,
        val avviksBeloep: Kroner,
        val utbetaltBeloep: Kroner,
        val resultatType: EtteroppgjoerResultatType,
        val stoenad: Kroner,
        val faktiskStoenad: Kroner,
        val grunnlag: EtteroppgjoerBrevGrunnlag,
        val rettsgebyrBeloep: Kroner,
        val harOpphoer: Boolean,
    ) : BrevFastInnholdData() {
        override val type: String = "OMS_EO_VEDTAK"

        override fun medVedleggInnhold(innhold: () -> List<BrevInnholdVedlegg>): BrevFastInnholdData =
            this.copy(
                vedleggInnhold =
                    krevIkkeNull(
                        innhold()
                            .singleOrNull {
                                it.key == BrevVedleggKey.OMS_EO_BEREGNINGSVEDLEGG
                            }?.payload,
                    ) {
                        "Mangler påkrevd vedlegg for etteroppgjør beregningsvedlegg"
                    }.elements,
            )

        override val brevKode: Brevkoder = Brevkoder.OMS_EO_VEDTAK
    }

    data class VedtakInnhold(
        val etteroppgjoersAar: Int,
        val forhaandsvarselSendtDato: LocalDate?,
        val mottattSvarDato: LocalDate?,
    ) : BrevRedigerbarInnholdData() {
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
    val inntekt: Kroner,
    val pensjonsgivendeInntektHeleAaret: Kroner,
) {
    companion object {
        fun fra(
            grunnlag: FaktiskInntektDto,
            pensjonsgivendeInntektHeleAaret: Int?,
        ): EtteroppgjoerBrevGrunnlag {
            krevIkkeNull(grunnlag.inntektInnvilgetPeriode) {
                "Kan ikke vise beregningstabell uten summert faktisk inntekt"
            }

            return EtteroppgjoerBrevGrunnlag(
                fom = grunnlag.fom,
                tom = grunnlag.tom!!,
                innvilgedeMaaneder = grunnlag.innvilgaMaaneder,
                loennsinntekt = Kroner(grunnlag.loennsinntekt),
                naeringsinntekt = Kroner(grunnlag.naeringsinntekt),
                afp = Kroner(grunnlag.afp),
                utlandsinntekt = Kroner(grunnlag.utlandsinntekt),
                inntekt = Kroner(grunnlag.inntektInnvilgetPeriode!!),
                pensjonsgivendeInntektHeleAaret = Kroner(pensjonsgivendeInntektHeleAaret ?: 0),
            )
        }
    }
}
