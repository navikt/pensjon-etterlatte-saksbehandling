package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServerUtenRest

fun main() {
    // sikre is_healthy / is_alive
    initEmbeddedServerUtenRest(
        httpPort = 8080,
        applicationConfig = ConfigFactory.load(),
    )
}
