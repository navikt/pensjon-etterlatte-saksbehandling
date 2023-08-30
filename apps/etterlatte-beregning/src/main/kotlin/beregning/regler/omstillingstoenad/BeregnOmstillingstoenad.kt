package no.nav.etterlatte.beregning.regler.omstillingstoenad

import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.beregning.regler.omstillingstoenad.sats.omstillingsstoenadSatsRegel
import no.nav.etterlatte.beregning.regler.omstillingstoenad.trygdetidsfaktor.trygdetidsFaktor
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.PeriodisertGrunnlag
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import no.nav.etterlatte.regler.Beregningstall
import java.time.LocalDate

data class Avdoed(val trygdetid: Beregningstall)

data class PeriodisertOmstillingstoenadGrunnlag(
    val avdoed: PeriodisertGrunnlag<FaktumNode<Avdoed>>,
    val institusjonsopphold: PeriodisertGrunnlag<FaktumNode<InstitusjonsoppholdBeregningsgrunnlag?>>
) : PeriodisertGrunnlag<OmstillingstoenadGrunnlag> {
    override fun finnAlleKnekkpunkter(): Set<LocalDate> {
        return avdoed.finnAlleKnekkpunkter() +
            institusjonsopphold.finnAlleKnekkpunkter()
    }

    override fun finnGrunnlagForPeriode(datoIPeriode: LocalDate): OmstillingstoenadGrunnlag {
        return OmstillingstoenadGrunnlag(
            avdoed.finnGrunnlagForPeriode(datoIPeriode),
            institusjonsopphold.finnGrunnlagForPeriode(datoIPeriode)
        )
    }
}

data class OmstillingstoenadGrunnlag(
    val avdoed: FaktumNode<Avdoed>,
    val institusjonsopphold: FaktumNode<InstitusjonsoppholdBeregningsgrunnlag?>
)

val beregnOmstillingsstoenadRegel = RegelMeta(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Reduserer ytelsen mot opptjening i folketrygden",
    regelReferanse = RegelReferanse(id = "OMS-BEREGNING-2024-REDUSER-MOT-TRYGDETID")
) benytter omstillingsstoenadSatsRegel og trygdetidsFaktor med { sats, trygdetidsfaktor ->
    sats.multiply(trygdetidsfaktor)
}

val kroneavrundetOmstillingsstoenadRegel = RegelMeta(
    gjelderFra = OMS_GYLDIG_FROM_TEST,
    beskrivelse = "Gjør en kroneavrunding av beregningen",
    regelReferanse = RegelReferanse(id = "REGEL-KRONEAVRUNDING")
) benytter beregnOmstillingsstoenadRegel med { beregnetOmstillingsstoenad ->
    beregnetOmstillingsstoenad.round(decimals = 0).toInteger()
}