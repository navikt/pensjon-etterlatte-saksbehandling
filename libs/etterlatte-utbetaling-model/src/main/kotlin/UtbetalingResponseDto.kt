package no.nav.etterlatte.libs.common.utbetaling

import java.util.UUID

data class UtbetalingResponseDto(
    val status: UtbetalingStatusDto,
    val vedtakId: Long? = null,
    val behandlingId: UUID? = null,
    val feilmelding: String? = null,
)

enum class UtbetalingStatusDto {
    GODKJENT,
    GODKJENT_MED_FEIL,
    AVVIST,
    FEILET,
    SENDT,
    MOTTATT,
}
