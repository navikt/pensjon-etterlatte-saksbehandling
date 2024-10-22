package no.nav.etterlatte.libs.common.inntektsjustering

import no.nav.etterlatte.libs.common.sak.SakId
import java.time.Year
import java.time.YearMonth

data class AarligInntektsjusteringRequest(
    val kjoering: String,
    val loependeFom: YearMonth,
    val saker: List<SakId>,
)

object AarligInntektsjusteringKjoering {
    fun getKjoering(): String = "Årlig inntektsjustering ${Year.now().plusYears(1)}"
}
