package no.nav.etterlatte.utbetaling.common

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingResponseDto

data class UtbetalingEventDto(
    @JsonProperty(EVENT_NAME_KEY) val eventName: String = UtbetalinghendelseType.OPPDATERT.lagEventnameForType(),
    @JsonProperty(UTBETALING_RESPONSE) val utbetalingResponse: UtbetalingResponseDto,
)
