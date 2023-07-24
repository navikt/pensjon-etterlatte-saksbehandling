package no.nav.etterlatte

import no.nav.etterlatte.jobs.addShutdownHook
import no.nav.etterlatte.statistikk.config.ApplicationContext
import no.nav.etterlatte.statistikk.config.sikkerLogg
import no.nav.helse.rapids_rivers.RapidsConnection

fun main() {
    with(ApplicationContext()) {
        jobs(this)
        rapidApplication(this).start()
        sikkerLogg.info("Sikkerlogg fra etterlatte-statistikk")
    }
}

fun jobs(applicationContext: ApplicationContext) {
    applicationContext.maanedligStatistikkJob.schedule().also { addShutdownHook(it) }
}

fun rapidApplication(applicationContext: ApplicationContext): RapidsConnection =
    applicationContext.rapidsConnection
        .apply {
            applicationContext.vedtakhendelserRiver
            applicationContext.behandlinghendelseRiver
            applicationContext.soeknadStatistikkRiver
        }