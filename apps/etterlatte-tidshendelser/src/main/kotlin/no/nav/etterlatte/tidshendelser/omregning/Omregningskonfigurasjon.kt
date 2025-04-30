package no.nav.etterlatte.tidshendelser.omregning

import no.nav.etterlatte.libs.common.sak.SakId
import java.time.LocalDate

data class Omregningskonfigurasjon(
    val antall: Int,
    val dato: LocalDate,
    val spesifikkeSaker: List<SakId>,
    val ekskluderteSaker: List<SakId>,
    val kjoeringId: String? = null,
)
