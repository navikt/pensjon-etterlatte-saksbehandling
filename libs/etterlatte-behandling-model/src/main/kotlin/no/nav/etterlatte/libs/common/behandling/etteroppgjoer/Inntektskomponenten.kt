package no.nav.etterlatte.libs.common.behandling.etteroppgjoer

import java.math.BigDecimal
import java.time.YearMonth

data class InntektSummert(
    val filter: String,
    val inntekter: List<Inntektsmaaned>,
)

data class Inntektsmaaned(
    val maaned: YearMonth,
    val beloep: BigDecimal,
)
