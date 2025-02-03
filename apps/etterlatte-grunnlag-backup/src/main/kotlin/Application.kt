package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServer
import no.nav.etterlatte.libs.ktor.initialisering.run

fun main() {
    // sikre is_healthy / is_alive
    initEmbeddedServer(
        httpPort = 8080,
        applicationConfig = ConfigFactory.load(),
    ) {
        // run
    }.run()
}
