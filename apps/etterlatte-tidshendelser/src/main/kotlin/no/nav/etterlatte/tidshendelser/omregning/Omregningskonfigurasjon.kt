package no.nav.etterlatte.tidshendelser.omregning

import no.nav.etterlatte.libs.common.sak.SakId
import java.time.LocalDate
import java.time.YearMonth

data class Omregningskonfigurasjon(
    val antall: Int,
    val datoVirkFom: YearMonth,
    val spesifikkeSaker: List<SakId>,
    val ekskluderteSaker: List<SakId>,
    val kjoeringId: String? = null,
)
