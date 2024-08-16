package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.Application
import io.ktor.server.config.HoconApplicationConfig
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.migrering.FeilendeMigreringLytterRiver
import no.nav.etterlatte.migrering.LyttPaaDistribuerBrevRiver
import no.nav.etterlatte.migrering.LyttPaaIverksattVedtakRiver
import no.nav.etterlatte.migrering.PesysRepository
import no.nav.etterlatte.migrering.migreringRoute
import rapidsandrivers.initRogR

fun main() = Server().run()

internal class Server {
    private val dataSource = DataSourceBuilder.createDataSource(Miljoevariabler.systemEnv())
    private val pesysRepository = PesysRepository(dataSource)

    fun run() {
        dataSource.migrate()
        val restModule: Application.() -> Unit = {
            restModule(
                sikkerLogg = sikkerlogger(),
                config = HoconApplicationConfig(ConfigFactory.load()),
            ) {
                migreringRoute(pesysRepository)
            }
        }
        initRogR("migrering", restModule) { rapidsConnection, _ ->
            LyttPaaIverksattVedtakRiver(rapidsConnection, pesysRepository)
            LyttPaaDistribuerBrevRiver(rapidsConnection, pesysRepository)
            FeilendeMigreringLytterRiver(rapidsConnection, pesysRepository)
        }
    }
}
