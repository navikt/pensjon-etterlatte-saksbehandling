package no.nav.etterlatte

import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstart
import no.nav.etterlatte.statistikk.config.ApplicationContext

fun main() =
    with(ApplicationContext()) {
        maanedligStatistikkJob.schedule().also { addShutdownHook(it) }
        initRapidsConnection()
        sikkerLoggOppstart("etterlatte-statistikk")
    }
