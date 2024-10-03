package no.nav.etterlatte.tidshendelser.regulering

import no.nav.etterlatte.libs.common.sak.SakId
import java.time.LocalDate

data class Reguleringskonfigurasjon(
    val antall: Int,
    val dato: LocalDate,
    val spesifikkeSaker: List<SakId>,
    val ekskluderteSaker: List<SakId>,
)
