package no.nav.etterlatte

import io.ktor.server.cio.CIO
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.trygdetid.avtale.avtale
import no.nav.etterlatte.trygdetid.config.ApplicationContext
import no.nav.etterlatte.trygdetid.kodeverk
import no.nav.etterlatte.trygdetid.trygdetid
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val sikkerLogg: Logger = LoggerFactory.getLogger("sikkerLogg")

fun main() {
    ApplicationContext().let { Server(it).run() }
}

class Server(private val context: ApplicationContext) {
    init {
        sikkerLogg.info("SikkerLogg: etterlatte-trygdetid oppstart")
    }

    private val engine = with(context) {
        embeddedServer(
            factory = CIO,
            environment = applicationEngineEnvironment {
                config = HoconApplicationConfig(context.config)
                module {
                    restModule(sikkerLogg, withMetrics = true) {
                        trygdetid(trygdetidService, behandlingKlient)
                        avtale(avtaleService, behandlingKlient)
                        kodeverk(kodeverkService)
                    }
                }
                connector { port = properties.httpPort }
            }
        )
    }

    fun run() = with(context) {
        dataSource.migrate()
        setReady()
        engine.start(true)
    }
}