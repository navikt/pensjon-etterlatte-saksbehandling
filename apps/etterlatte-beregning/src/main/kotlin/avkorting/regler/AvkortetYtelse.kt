package no.nav.etterlatte.avkorting.regler

import no.nav.etterlatte.beregning.regler.omstillingstoenad.OMS_GYLDIG_FROM_TEST
import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.KonstantGrunnlag
import no.nav.etterlatte.libs.regler.PeriodisertGrunnlag
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import no.nav.etterlatte.regler.Beregningstall
import java.time.LocalDate
import kotlin.math.max

data class PeriodisertAvkortetYtelseGrunnlag(
    val beregningsperioder: PeriodisertGrunnlag<FaktumNode<Int>>,
    val avkortingsperioder: PeriodisertGrunnlag<FaktumNode<Int>>,
    val fordeltRestanse: KonstantGrunnlag<FaktumNode<Int>>,
) : PeriodisertGrunnlag<AvkortetYtelseGrunnlag> {
    override fun finnAlleKnekkpunkter(): Set<LocalDate> {
        return beregningsperioder.finnAlleKnekkpunkter() + avkortingsperioder.finnAlleKnekkpunkter()
    }

    override fun finnGrunnlagForPeriode(datoIPeriode: LocalDate): AvkortetYtelseGrunnlag {
        return AvkortetYtelseGrunnlag(
            beregning = beregningsperioder.finnGrunnlagForPeriode(datoIPeriode),
            avkorting = avkortingsperioder.finnGrunnlagForPeriode(datoIPeriode),
            fordeltRestanse = fordeltRestanse.finnGrunnlagForPeriode(datoIPeriode),
        )
    }
}

data class AvkortetYtelseGrunnlag(
    val beregning: FaktumNode<Int>,
    val avkorting: FaktumNode<Int>,
    val fordeltRestanse: FaktumNode<Int>,
)

val beregningsbeloep: Regel<AvkortetYtelseGrunnlag, Int> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FROM_TEST,
        beskrivelse = "Finner beregnet ytelse før avkorting",
        finnFaktum = AvkortetYtelseGrunnlag::beregning,
        finnFelt = { it },
    )

val avkortingsbeloep: Regel<AvkortetYtelseGrunnlag, Int> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FROM_TEST,
        beskrivelse = "Finner avkortignsbeløp",
        finnFaktum = AvkortetYtelseGrunnlag::avkorting,
        finnFelt = { it },
    )

val fordeltRestanseGrunnlag: Regel<AvkortetYtelseGrunnlag, Int> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FROM_TEST,
        beskrivelse = "Restanse fordelt over gjenværende måneder for gjeldende år",
        finnFaktum = AvkortetYtelseGrunnlag::fordeltRestanse,
        finnFelt = { it },
    )

val avkorteYtelse =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FROM_TEST,
        beskrivelse = "Finner endelig ytelse ved å trekke avkortingsbeløp fra beregnet ytelse",
        regelReferanse = RegelReferanse(id = "REGEL-AVKORTET-YTELSE"),
    ) benytter beregningsbeloep og avkortingsbeloep med { beregningsbeloep, avkortingsbeloep ->
        Beregningstall(beregningsbeloep).minus(Beregningstall(avkortingsbeloep))
    }

val avkortetYtelseMedRestanse =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FROM_TEST,
        beskrivelse = "Legger til restanse fra endret avkorting til tidligere måneder",
        regelReferanse = RegelReferanse(id = "REGEL-AVKORTET-YTELSE-MED-RESTANSE"),
    ) benytter avkorteYtelse og fordeltRestanseGrunnlag med { avkorteYtelse, fordeltRestanse ->
        avkorteYtelse.minus(Beregningstall(fordeltRestanse))
            .toInteger()
            .let { max(it, 0) }
    }
