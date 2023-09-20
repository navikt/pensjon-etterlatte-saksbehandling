package no.nav.etterlatte.pdl

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import java.time.LocalDate

data class ForeldreansvarPeriode(
    val fraDato: LocalDate?,
    val tilDato: LocalDate?,
    val forelder: Folkeregisteridentifikator,
)

data class HistorikkForeldreansvar(
    val ansvarligeForeldre: List<ForeldreansvarPeriode>,
    val foreldre: List<Folkeregisteridentifikator>,
)
