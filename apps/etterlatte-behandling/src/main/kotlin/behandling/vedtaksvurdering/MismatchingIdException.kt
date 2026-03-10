package no.nav.etterlatte.behandling.vedtaksvurdering

import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException

class MismatchingIdException(
    message: String,
) : ForespoerselException(
        HttpStatusCode.Companion.BadRequest.value,
        "ID_MISMATCH_MELLOM_PATH_OG_BODY",
        message,
    )
