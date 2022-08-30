package no.nav.etterlatte.libs.common.utbetaling

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

const val EVENT_NAME_OPPDATERT = "UTBETALING:OPPDATERT"

data class UtbetalingEventDto(
    @JsonProperty("@event_name") val event: String = EVENT_NAME_OPPDATERT,
    @JsonProperty("utbetaling_response") val utbetalingResponse: UtbetalingResponseDto
)

data class UtbetalingResponseDto(
    val status: UtbetalingStatusDto,
    val vedtakId: Long? = null,
    val behandlingId: UUID? = null,
    val feilmelding: String? = null
)

enum class UtbetalingStatusDto {
    GODKJENT, GODKJENT_MED_FEIL, AVVIST, FEILET, SENDT, MOTTATT
}