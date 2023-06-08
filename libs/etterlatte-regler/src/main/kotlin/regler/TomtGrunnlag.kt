package no.nav.etterlatte.libs.regler

import java.time.LocalDate

class TomtGrunnlag<T>(private val default: T) : PeriodisertGrunnlag<T> {
    override fun finnAlleKnekkpunkter(): Set<LocalDate> = setOf()

    override fun finnGrunnlagForPeriode(datoIPeriode: LocalDate) = default
}