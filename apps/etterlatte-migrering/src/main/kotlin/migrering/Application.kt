package no.nav.etterlatte

import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.migrering.ApplicationContext
import no.nav.etterlatte.migrering.FeilendeMigreringLytterRiver
import no.nav.etterlatte.migrering.LagreKoblingRiver
import no.nav.etterlatte.migrering.LyttPaaIverksattVedtakRiver
import no.nav.etterlatte.migrering.MigrerSpesifikkSakRiver
import no.nav.etterlatte.migrering.MigreringRiver
import no.nav.helse.rapids_rivers.RapidApplication
import rapidsandrivers.getRapidEnv

fun main() = ApplicationContext().let { Server(it).run() }

internal class Server(private val context: ApplicationContext) {
    init {
        sikkerLoggOppstartOgAvslutning("etterlatte-migrering")
    }

    fun run() =
        with(context) {
            dataSource.migrate()
            val rapidEnv = getRapidEnv()
            RapidApplication.create(rapidEnv).also { rapidsConnection ->
                MigreringRiver(rapidsConnection)
                MigrerSpesifikkSakRiver(rapidsConnection, penklient, pesysRepository, featureToggleService, verifiserer)
                LagreKoblingRiver(rapidsConnection, pesysRepository)
                LyttPaaIverksattVedtakRiver(rapidsConnection, pesysRepository, penklient, featureToggleService)
                FeilendeMigreringLytterRiver(rapidsConnection, pesysRepository)
            }.start()
        }
}
