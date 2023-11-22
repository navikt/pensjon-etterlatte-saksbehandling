package no.nav.etterlatte

import migrering.migreringRoute
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.migrering.ApplicationContext
import no.nav.etterlatte.migrering.FeilendeMigreringLytterRiver
import no.nav.etterlatte.migrering.LagreKoblingRiver
import no.nav.etterlatte.migrering.LyttPaaIverksattVedtakRiver
import no.nav.etterlatte.migrering.PauseMigreringRiver
import no.nav.etterlatte.migrering.start.MigrerSpesifikkSakRiver
import no.nav.etterlatte.migrering.start.MigreringRiver
import no.nav.etterlatte.migrering.start.StartMigrering
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

            RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(rapidEnv))
                .withKtorModule {
                    restModule(
                        sikkerLogg = sikkerlogger(),
                    ) {
                        migreringRoute(pesysRepository)
                    }
                }
                .build()
                .also { rapidsConnection ->
                    MigreringRiver(rapidsConnection)
                    MigrerSpesifikkSakRiver(
                        rapidsConnection,
                        penklient,
                        pesysRepository,
                        featureToggleService,
                        verifiserer,
                        krrKlient,
                    )
                    LagreKoblingRiver(rapidsConnection, pesysRepository)
                    PauseMigreringRiver(rapidsConnection, pesysRepository)
                    LyttPaaIverksattVedtakRiver(rapidsConnection, pesysRepository, penklient, featureToggleService)
                    FeilendeMigreringLytterRiver(rapidsConnection, pesysRepository)
                    StartMigrering(startMigreringRepository, rapidsConnection)
                }.start()
        }
}
