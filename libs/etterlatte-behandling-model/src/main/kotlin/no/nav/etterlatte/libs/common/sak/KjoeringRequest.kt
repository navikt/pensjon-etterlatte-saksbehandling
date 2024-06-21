package no.nav.etterlatte.libs.common.sak

import java.math.BigDecimal

data class KjoeringRequest(
    val kjoering: String,
    val status: KjoeringStatus,
    val sakId: Long,
)

data class LagreKjoeringRequest(
    val kjoering: String,
    val status: KjoeringStatus,
    val sakId: Long,
    val beregningBeloepFoer: BigDecimal,
    val beregningBeloepEtter: BigDecimal,
    val beregningGFoer: BigDecimal,
    val beregningGEtter: BigDecimal,
    val beregningBruktOmregningsfaktor: BigDecimal,
    val vedtakBeloep: BigDecimal,
)

enum class KjoeringStatus {
    KLAR_TIL_REGULERING,
    STARTA,
    FEILA,
    IKKE_LOEPENDE,
    FERDIGSTILT,
}
