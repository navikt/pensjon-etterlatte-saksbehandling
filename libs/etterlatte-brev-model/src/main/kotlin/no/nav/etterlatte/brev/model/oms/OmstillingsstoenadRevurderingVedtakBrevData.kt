package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.BrevData
import no.nav.etterlatte.brev.BrevFastInnholdData
import no.nav.etterlatte.brev.BrevInnholdVedlegg
import no.nav.etterlatte.brev.BrevRedigerbarInnholdData
import no.nav.etterlatte.brev.BrevVedleggInnholdData
import no.nav.etterlatte.brev.BrevVedleggKey
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.Vedlegg
import no.nav.etterlatte.brev.model.FeilutbetalingType
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregning
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregningsperiode
import no.nav.etterlatte.brev.model.OmstillingsstoenadEtterbetaling
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

object OmstillingsstoenadRevurderingVedtakBrevData {
    data class Vedtak(
        val erEndret: Boolean,
        val erOmgjoering: Boolean,
        val datoVedtakOmgjoering: LocalDate?,
        val beregning: OmstillingsstoenadBeregning,
        val omsRettUtenTidsbegrensning: Boolean,
        val feilutbetaling: FeilutbetalingType,
        val bosattUtland: Boolean,
        val erInnvilgelsesaar: Boolean,
        val tidligereFamiliepleier: Boolean,
        val innholdForhaandsvarsel: List<Slate.Element> = emptyList(),
    ) : BrevFastInnholdData() {
        override val type: String = "OMSTILLINGSSTOENAD_REVURDERING"

        init {
            if (erOmgjoering && datoVedtakOmgjoering == null) {
                throw InternfeilException(
                    "Kunne ikke lage revurderingsbrevet for omstillingsstønad siden vi ikke" +
                        " fikk dato vedtak for omgjøring, i en revurdering som er omgjøring.",
                )
            }
        }

        override fun medVedleggInnhold(innhold: () -> List<BrevInnholdVedlegg>): BrevFastInnholdData =
            this.copy(
                beregning =
                    beregning.copy(
                        innhold =
                            innhold()
                                .singleOrNull { it.key == BrevVedleggKey.OMS_BEREGNING }
                                ?.let { it.payload!!.elements }
                                ?: emptyList(),
                    ),
                innholdForhaandsvarsel =
                    innhold()
                        .takeIf { feilutbetaling == FeilutbetalingType.FEILUTBETALING_MED_VARSEL }
                        ?.single { it.key == BrevVedleggKey.OMS_FORHAANDSVARSEL_FEILUTBETALING }
                        ?.let { it.payload!!.elements }
                        ?: emptyList(),
            )

        override val brevKode: Brevkoder = Brevkoder.OMS_REVURDERING
    }

    data class VedtakInnhold(
        val beregning: OmstillingsstoenadBeregningRedigerbartUtfall,
        val erEndret: Boolean,
        val erEtterbetaling: Boolean,
        val etterbetaling: OmstillingsstoenadEtterbetaling?,
        val feilutbetaling: FeilutbetalingType,
        val harFlereUtbetalingsperioder: Boolean,
        val harUtbetaling: Boolean,
        val inntekt: Kroner,
        val inntektsAar: Int,
        val mottattInntektendringAutomatisk: LocalDate?,
    ) : BrevRedigerbarInnholdData() {
        override val type: String = "OMSTILLINGSSTOENAD_REVURDERING_UTFALL"

        override val brevKode: Brevkoder = Brevkoder.OMS_REVURDERING
    }

    data class VedtakAarligInntektsjustering(
        val beregning: OmstillingsstoenadBeregning,
        val omsRettUtenTidsbegrensning: Boolean = false,
        val tidligereFamiliepleier: Boolean = false,
        val inntektsaar: Int,
        val harUtbetaling: Boolean,
        val endringIUtbetaling: Boolean,
        val virkningstidspunkt: LocalDate,
        val bosattUtland: Boolean,
    ) : BrevFastInnholdData() {
        override val brevKode: Brevkoder = Brevkoder.OMS_INNTEKTSJUSTERING_VEDTAK
        override val type: String = "OMS_AARLIG_INNTEKTSJUSTERING"

        override fun medVedleggInnhold(innhold: () -> List<BrevInnholdVedlegg>): BrevFastInnholdData =
            this.copy(
                beregning =
                    beregning.copy(
                        innhold =
                            innhold()
                                .single {
                                    it.key == BrevVedleggKey.OMS_BEREGNING
                                }.payload!!
                                .elements,
                    ),
            )
    }

    data class VedtakInnholdAarligInntektsjustering(
        val inntektsbeloep: Kroner,
        val inntektsaar: Int,
    ) : BrevRedigerbarInnholdData() {
        override val brevKode: Brevkoder = Brevkoder.OMS_INNTEKTSJUSTERING_VEDTAK
        override val type: String = "OMS_AARLIG_INNTEKTSJUSTERING_UTFALL"
    }

    data class VedtakOpphoer(
        val bosattUtland: Boolean,
        val virkningsdato: LocalDate,
        val feilutbetaling: FeilutbetalingType,
        val innholdForhaandsvarsel: List<Slate.Element> = emptyList(),
    ) : BrevFastInnholdData() {
        override val brevKode: Brevkoder = Brevkoder.OMS_OPPHOER
        override val type: String = "OMSTILLINGSSTOENAD_OPPHOER"

        override fun medVedleggInnhold(innhold: () -> List<BrevInnholdVedlegg>): BrevFastInnholdData =
            this.copy(
                innholdForhaandsvarsel =
                    innhold()
                        .takeIf { feilutbetaling == FeilutbetalingType.FEILUTBETALING_MED_VARSEL }
                        ?.single { it.key == BrevVedleggKey.OMS_FORHAANDSVARSEL_FEILUTBETALING }
                        ?.let { it.payload!!.elements }
                        ?: emptyList(),
            )
    }

    data class VedtakInnholdOpphoer(
        val feilutbetaling: FeilutbetalingType,
    ) : BrevRedigerbarInnholdData() {
        override val brevKode: Brevkoder = Brevkoder.OMS_OPPHOER
        override val type: String = "OMSTILLINGSSTOENAD_OPPHOER"
    }
}

data class OmstillingsstoenadBeregningRedigerbartUtfall(
    val virkningsdato: LocalDate,
    val beregningsperioder: List<OmstillingsstoenadBeregningsperiode>,
    val sisteBeregningsperiode: OmstillingsstoenadBeregningsperiode,
    val sisteBeregningsperiodeNesteAar: OmstillingsstoenadBeregningsperiode?,
    val oppphoersdato: LocalDate?,
    val opphoerNesteAar: Boolean,
) : BrevData

data class OmstillingsstoenadBeregningRedigerbartVedleggData(
    val omstillingsstoenadBeregning: OmstillingsstoenadBeregning,
    val erInnvilgelsesAar: Boolean,
) : BrevVedleggInnholdData() {
    override val type: String = "OMS_BEREGNINGVEDLEGG_UTFALL"
    override val brevKode: Vedlegg = Vedlegg.OMSTILLINGSSTOENAD_VEDLEGG_BEREGNING_UTFALL
}
