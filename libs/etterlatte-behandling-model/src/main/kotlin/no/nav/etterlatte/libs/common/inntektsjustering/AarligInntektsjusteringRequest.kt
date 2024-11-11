package no.nav.etterlatte.libs.common.inntektsjustering

import no.nav.etterlatte.libs.common.sak.SakId
import java.time.YearMonth

data class AarligInntektsjusteringRequest(
    val kjoering: String,
    val loependeFom: YearMonth,
    val saker: List<SakId>,
)
