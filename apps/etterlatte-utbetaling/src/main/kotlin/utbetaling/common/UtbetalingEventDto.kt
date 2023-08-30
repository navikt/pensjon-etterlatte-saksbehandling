package no.nav.etterlatte.utbetaling.common

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingResponseDto

data class UtbetalingEventDto(
    @JsonProperty("@event_name") val event: String = EVENT_NAME_OPPDATERT,
    @JsonProperty("utbetaling_response") val utbetalingResponse: UtbetalingResponseDto
)