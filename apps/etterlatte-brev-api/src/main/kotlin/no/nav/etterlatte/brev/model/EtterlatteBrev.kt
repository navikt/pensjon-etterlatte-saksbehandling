package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Trygdetidsperiode
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

data class BarnepensjonEtterbetaling(
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val etterbetalingsperioder: List<BarnepensjonBeregningsperiode>,
)

data class OmstillingsstoenadEtterbetaling(
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val etterbetalingsperioder: List<OmstillingsstoenadBeregningsperiode>,
)

data class BarnepensjonBeregning(
    override val innhold: List<Slate.Element>,
    val antallBarn: Int,
    val virkningsdato: LocalDate,
    val grunnbeloep: Kroner,
    val beregningsperioder: List<BarnepensjonBeregningsperiode>,
    val sisteBeregningsperiode: BarnepensjonBeregningsperiode,
    val trygdetid: TrygdetidMedBeregningsmetode,
    val erForeldreloes: Boolean = false,
    val bruktAvdoed: String? = null,
) : BrevdataMedInnhold

data class BarnepensjonBeregningsperiode(
    val datoFOM: LocalDate,
    val datoTOM: LocalDate?,
    val grunnbeloep: Kroner,
    val antallBarn: Int,
    var utbetaltBeloep: Kroner,
)

data class OmstillingsstoenadBeregning(
    override val innhold: List<Slate.Element>,
    val virkningsdato: LocalDate,
    val inntekt: Kroner,
    val aarsInntekt: Kroner,
    val fratrekkInnAar: Kroner,
    val gjenvaerendeMaaneder: Int,
    val grunnbeloep: Kroner,
    val beregningsperioder: List<OmstillingsstoenadBeregningsperiode>,
    val sisteBeregningsperiode: OmstillingsstoenadBeregningsperiode,
    val trygdetid: TrygdetidMedBeregningsmetode,
) : BrevdataMedInnhold

data class OmstillingsstoenadBeregningsperiode(
    val datoFOM: LocalDate,
    val datoTOM: LocalDate?,
    val inntekt: Kroner,
    val ytelseFoerAvkorting: Kroner,
    val utbetaltBeloep: Kroner,
    val trygdetid: Int,
)

data class TrygdetidMedBeregningsmetode(
    val trygdetidsperioder: List<Trygdetidsperiode>,
    val beregnetTrygdetidAar: Int,
    val beregnetTrygdetidMaaneder: Int,
    val prorataBroek: IntBroek?,
    val beregningsMetodeAnvendt: BeregningsMetode,
    val beregningsMetodeFraGrunnlag: BeregningsMetode,
    val mindreEnnFireFemtedelerAvOpptjeningstiden: Boolean,
)

enum class FeilutbetalingType {
    FEILUTBETALING_UTEN_VARSEL,
    FEILUTBETALING_MED_VARSEL,
    INGEN_FEILUTBETALING,
}
