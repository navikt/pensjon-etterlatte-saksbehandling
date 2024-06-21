package no.nav.etterlatte

import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.statistikk.config.ApplicationContext
import no.nav.helse.rapids_rivers.RapidsConnection

fun main() {
    with(ApplicationContext()) {
        jobs(this)
        rapidApplication(this).start()
        sikkerLoggOppstartOgAvslutning("etterlatte-statistikk")
    }
}

fun jobs(applicationContext: ApplicationContext) {
    applicationContext.maanedligStatistikkJob.schedule().also { addShutdownHook(it) }
    applicationContext.oppdaterVedtakJob.schedule().also { addShutdownHook(it) }
}

fun rapidApplication(applicationContext: ApplicationContext): RapidsConnection =
    applicationContext.rapidsConnection
        .apply {
            applicationContext.vedtakhendelserRiver
            applicationContext.avbruttOpprettetBehandlinghendelseRiver
            applicationContext.behandlingPaaVentHendelseRiver
            applicationContext.tilbakekrevingriver
            applicationContext.soeknadStatistikkRiver
            applicationContext.klageHendelseRiver
        }
