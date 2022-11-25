package no.nav.etterlatte

import no.nav.etterlatte.statistikk.config.ApplicationContext
import no.nav.helse.rapids_rivers.RapidsConnection
import rapidsandrivers.vedlikehold.registrerVedlikeholdsriver

fun main() {
    with(ApplicationContext()) {
        rapidApplication(this).start()
    }
}

fun rapidApplication(applicationContext: ApplicationContext): RapidsConnection =
    applicationContext.rapidsConnection
        .apply {
            applicationContext.vedtakhendelserRiver
            applicationContext.behandlinghendelseRiver
            registrerVedlikeholdsriver(applicationContext.statistikkService)
        }