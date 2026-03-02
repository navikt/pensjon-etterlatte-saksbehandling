package no.nav.etterlatte.libs.common.sak

import no.nav.etterlatte.libs.common.periode.Periode
import java.math.BigDecimal
import java.util.UUID

data class KjoeringRequest(
    val kjoering: String,
    val status: KjoeringStatus,
    val sakId: SakId,
    val begrunnelse: String? = null,
    val corrId: String? = null,
    val feilendeSteg: String? = null,
)

data class KjoeringDistEllerIverksattRequest(
    val kjoering: String,
    val sakId: SakId,
    val distEllerIverksatt: DisttribuertEllerIverksatt,
)

data class LagreKjoeringRequest(
    val kjoering: String,
    val status: KjoeringStatus,
    val sakId: SakId,
    val behandling: UUID? = null,
    val beregningBeloepFoer: BigDecimal?,
    val beregningBeloepEtter: BigDecimal?,
    val beregningGFoer: BigDecimal?,
    val beregningGEtter: BigDecimal?,
    val beregningBruktOmregningsfaktor: BigDecimal?,
    val avkortingFoer: BigDecimal?,
    val avkortingEtter: BigDecimal?,
    val vedtakBeloep: BigDecimal?,
    val innvilgedePerioderFoer: List<Periode>?,
    val innvilgedePerioderEtter: List<Periode>?,
)

enum class KjoeringStatus {
    KLAR_TIL_REGULERING, // TODO bør denne fases ut da den er spesifikk for regulering?
    KLAR_FOR_OMREGNING,
    KLAR,
    STARTA,
    FEILA,
    IKKE_LOEPENDE, // Muligens ikke i bruk lenger?
    FERDIGSTILT,
    FERDIGSTILT_FATTET,
    TIL_MANUELL,
    TIL_MANUELL_UTEN_OPPGAVE,
    ;

    fun erFerdigstilt() =
        this in
            listOf(
                FERDIGSTILT,
                FERDIGSTILT_FATTET,
                TIL_MANUELL,
                TIL_MANUELL_UTEN_OPPGAVE,
                IKKE_LOEPENDE,
            )
}

enum class DisttribuertEllerIverksatt {
    IVERKSATT,
    DISTRIBUERT,
}
