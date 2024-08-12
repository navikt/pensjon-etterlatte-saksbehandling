package no.nav.etterlatte.libs.ktor

import io.ktor.http.HttpStatusCode
import java.util.concurrent.atomic.AtomicBoolean

private var ready = AtomicBoolean(false)

fun setReady(value: Boolean = true) {
    ready.set(value)
}

fun isReady() = if (ready.get()) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
