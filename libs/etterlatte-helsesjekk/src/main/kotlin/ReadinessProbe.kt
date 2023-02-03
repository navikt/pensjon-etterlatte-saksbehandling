package no.nav.etterlatte.libs.helsesjekk

import io.ktor.http.HttpStatusCode

private var ready = false

fun setReady() {
    ready = true
}

fun isReady() = if (ready) HttpStatusCode.OK else HttpStatusCode.ExpectationFailed