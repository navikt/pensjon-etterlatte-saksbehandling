package no.nav.etterlatte.libs.ktor.initialisering

import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import java.util.concurrent.atomic.AtomicBoolean

private var ready = AtomicBoolean(false)

fun setReady(value: Boolean = true) = ready.set(value)

fun isReady() =
    if (ready.get()) {
        HttpStatusCode.OK
    } else {
        throw ForespoerselException(
            status = HttpStatusCode.ServiceUnavailable.value,
            "SERVICE_UNAVAILABLE",
            "Service not yet ready",
        )
    }
