package no.nav.etterlatte.libs.ktor.ktor

import io.ktor.server.cio.CIOApplicationEngine

fun shutdownPolicyEmbeddedServer(): CIOApplicationEngine.Configuration.() -> Unit =
    {
        val tiSekunderInMIllis = 10000L
        val elleveSekunderInMIllis = 11000L
        shutdownGracePeriod = tiSekunderInMIllis
        shutdownTimeout = elleveSekunderInMIllis
    }
