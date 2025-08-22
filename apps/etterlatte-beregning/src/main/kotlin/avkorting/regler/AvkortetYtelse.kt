package no.nav.etterlatte.avkorting.regler

import no.nav.etterlatte.beregning.regler.omstillingstoenad.OMS_GYLDIG_FRA
import no.nav.etterlatte.libs.common.beregning.Sanksjon
import no.nav.etterlatte.libs.regler.FaktumNode
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
import kotlin.math.min

data class PeriodisertAvkortetYtelseGrunnlag(
    val beregningsperioder: PeriodisertGrunnlag<FaktumNode<Int>>,
    val avkortingsperioder: PeriodisertGrunnlag<FaktumNode<Int>>,
    val fordeltRestanse: PeriodisertGrunnlag<FaktumNode<Int>>,
    val sanksjonsperioder: PeriodisertGrunnlag<FaktumNode<Sanksjon?>>,
) : PeriodisertGrunnlag<AvkortetYtelseGrunnlag> {
    override fun finnAlleKnekkpunkter(): Set<LocalDate> =
        beregningsperioder.finnAlleKnekkpunkter() +
            avkortingsperioder.finnAlleKnekkpunkter() +
            fordeltRestanse.finnAlleKnekkpunkter() +
            sanksjonsperioder.finnAlleKnekkpunkter()

    override fun finnGrunnlagForPeriode(datoIPeriode: LocalDate): AvkortetYtelseGrunnlag =
        AvkortetYtelseGrunnlag(
            beregning = beregningsperioder.finnGrunnlagForPeriode(datoIPeriode),
            avkorting = avkortingsperioder.finnGrunnlagForPeriode(datoIPeriode),
            fordeltRestanse = fordeltRestanse.finnGrunnlagForPeriode(datoIPeriode),
            sanksjon = sanksjonsperioder.finnGrunnlagForPeriode(datoIPeriode),
        )
}

data class AvkortetYtelseGrunnlag(
    val beregning: FaktumNode<Int>,
    val avkorting: FaktumNode<Int>,
    val fordeltRestanse: FaktumNode<Int>,
    val sanksjon: FaktumNode<Sanksjon?>,
)

val beregningsbeloep: Regel<AvkortetYtelseGrunnlag, Int> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Finner beregnet ytelse før avkorting",
        finnFaktum = AvkortetYtelseGrunnlag::beregning,
        finnFelt = { it },
    )

val avkortingsbeloep: Regel<AvkortetYtelseGrunnlag, Int> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Finner avkortignsbeløp",
        finnFaktum = AvkortetYtelseGrunnlag::avkorting,
        finnFelt = { it },
    )

val fordeltRestanseGrunnlag: Regel<AvkortetYtelseGrunnlag, Int> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Restanse fordelt over gjenværende måneder for gjeldende år",
        finnFaktum = AvkortetYtelseGrunnlag::fordeltRestanse,
        finnFelt = { it },
    )

val harSanksjon: Regel<AvkortetYtelseGrunnlag, Sanksjon?> =
    finnFaktumIGrunnlag(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Har sanksjon",
        finnFaktum = AvkortetYtelseGrunnlag::sanksjon,
        finnFelt = { it },
    )

val avkorteYtelse =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Finner endelig ytelse ved å trekke avkortingsbeløp fra beregnet ytelse",
        regelReferanse = RegelReferanse(id = "REGEL-AVKORTET-YTELSE"),
    ) benytter beregningsbeloep og avkortingsbeloep med { beregningsbeloep, avkortingsbeloep ->
        Beregningstall(beregningsbeloep).minus(Beregningstall(avkortingsbeloep))
    }

val avkortetYtelseMedRestanse =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Legger til restanse fra endret avkorting til tidligere måneder",
        regelReferanse = RegelReferanse(id = "REGEL-AVKORTET-YTELSE-MED-RESTANSE", versjon = "2"),
    ) benytter avkorteYtelse og fordeltRestanseGrunnlag med { avkorteYtelse, fordeltRestanse ->
        avkorteYtelse
            .minus(Beregningstall(fordeltRestanse))
            .toInteger()
    }

val avkortetYtelseMedRestanseMinMaks =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Utbetalt hver måned skal ikke være mindre enn 0 eller høyere en beregnet beløp",
        regelReferanse = RegelReferanse(id = "REGEL-AVKORTET-YTELSE-MAKS-MIN"),
    ) benytter avkortetYtelseMedRestanse og beregningsbeloep med { avkortetYtelseMedRestanse, beregningsbeloep ->
        min(max(avkortetYtelseMedRestanse, 0), beregningsbeloep)
    }

val avkortetYtelseMedRestanseOgSanksjon =
    RegelMeta(
        gjelderFra = OMS_GYLDIG_FRA,
        beskrivelse = "Setter 0 hvis sanksjon, ellers beregner avkortet ytelse med restanse",
        regelReferanse = RegelReferanse(id = "REGEL-AVKORTET-YTELSE-MED-RESTANSE-OG-SANKSJON", versjon = "2"),
    ) benytter avkortetYtelseMedRestanseMinMaks og harSanksjon med { avkorteYtelseMedRestanse, sanksjon ->
        if (sanksjon != null) {
            0
        } else {
            avkorteYtelseMedRestanse
        }
    }
