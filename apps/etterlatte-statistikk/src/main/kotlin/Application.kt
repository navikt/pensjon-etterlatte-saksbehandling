package no.nav.etterlatte

import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.statistikk.config.ApplicationContext

fun main() =
    with(ApplicationContext()) {
        maanedligStatistikkJob.schedule().also { addShutdownHook(it) }
        oppdaterVedtakJob.schedule().also { addShutdownHook(it) }
        initRapidsConnection()
        sikkerLoggOppstartOgAvslutning("etterlatte-statistikk")
    }
