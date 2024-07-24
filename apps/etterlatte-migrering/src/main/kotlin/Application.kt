package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.Application
import io.ktor.server.config.HoconApplicationConfig
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.migrering.ApplicationContext
import no.nav.etterlatte.migrering.FeilendeMigreringLytterRiver
import no.nav.etterlatte.migrering.LyttPaaDistribuerBrevRiver
import no.nav.etterlatte.migrering.LyttPaaIverksattVedtakRiver
import no.nav.etterlatte.migrering.migreringRoute
import rapidsandrivers.initRogR

fun main() = ApplicationContext().let { Server(it).run() }

internal class Server(
    private val context: ApplicationContext,
) {
    init {
        sikkerLoggOppstartOgAvslutning("etterlatte-migrering")
    }

    fun run() =
        with(context) {
            dataSource.migrate()
            val restModule: Application.() -> Unit = {
                restModule(
                    sikkerLogg = sikkerlogger(),
                    config = HoconApplicationConfig(ConfigFactory.load()),
                ) {
                    migreringRoute(pesysRepository)
                }
            }
            initRogR(restModule, { setReady() }) { rapidsConnection, _ ->
                LyttPaaIverksattVedtakRiver(rapidsConnection, pesysRepository)
                LyttPaaDistribuerBrevRiver(rapidsConnection, pesysRepository)
                FeilendeMigreringLytterRiver(rapidsConnection, pesysRepository)
            }
        }
}
