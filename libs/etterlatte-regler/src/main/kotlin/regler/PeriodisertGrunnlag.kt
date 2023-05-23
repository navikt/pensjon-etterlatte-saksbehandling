package no.nav.etterlatte.libs.regler

import java.time.LocalDate

interface PeriodisertGrunnlag<G> {
    fun finnAlleKnekkpunkter(): Set<LocalDate>

    fun finnKnekkpunkterInnenforPeriode(periode: RegelPeriode): Set<LocalDate> {
        return finnAlleKnekkpunkter().filter { knekkpunkt ->
            knekkpunkt >= periode.fraDato && (periode.tilDato?.let { it >= knekkpunkt } ?: true)
        }.toSet()
    }

    fun finnGrunnlagForPeriode(datoIPeriode: LocalDate): G
}

class KonstantGrunnlag<G>(private val konstantGrunnlag: G) : PeriodisertGrunnlag<G> {

    override fun finnAlleKnekkpunkter(): Set<LocalDate> = emptySet()
    override fun finnGrunnlagForPeriode(datoIPeriode: LocalDate): G = konstantGrunnlag
}