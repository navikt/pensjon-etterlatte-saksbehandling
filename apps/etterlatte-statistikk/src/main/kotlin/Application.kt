package no.nav.etterlatte

import no.nav.etterlatte.statistikk.config.ApplicationContext
import no.nav.helse.rapids_rivers.RapidsConnection

fun main() {
    with(ApplicationContext()) {
        jobs(this)
        rapidApplication(this).start()
        // test
    }
}

fun jobs(applicationContext: ApplicationContext) {
    applicationContext.maanedligStatistikkJob.schedule()
}

fun rapidApplication(applicationContext: ApplicationContext): RapidsConnection =
    applicationContext.rapidsConnection
        .apply {
            applicationContext.vedtakhendelserRiver
            applicationContext.behandlinghendelseRiver
            applicationContext.soeknadStatistikkRiver
        }