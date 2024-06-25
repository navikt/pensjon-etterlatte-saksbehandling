package no.nav.etterlatte.tidshendelser.regulering

import java.time.LocalDate

data class Reguleringskonfigurasjon(
    val antall: Int,
    val dato: LocalDate,
    val spesifikkeSaker: List<Long>,
    val ekskluderteSaker: List<Long>,
)
