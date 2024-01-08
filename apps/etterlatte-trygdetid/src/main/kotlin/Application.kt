package no.nav.etterlatte

import io.ktor.server.cio.CIO
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import no.nav.etterlatte.libs.common.logging.sikkerLoggOppstartOgAvslutning
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.database.migrate
import no.nav.etterlatte.libs.ktor.restModule
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.trygdetid.avtale.avtale
import no.nav.etterlatte.trygdetid.config.ApplicationContext
import no.nav.etterlatte.trygdetid.kodeverk
import no.nav.etterlatte.trygdetid.trygdetid
import no.nav.etterlatte.trygdetid.trygdetidV2
import org.slf4j.Logger

val sikkerLogg: Logger = sikkerlogger()

fun main() {
    ApplicationContext().let { Server(it).run() }
}

class Server(private val context: ApplicationContext) {
    init {
        sikkerLoggOppstartOgAvslutning("etterlatte-trygdetid")
    }

    private val engine =
        with(context) {
            embeddedServer(
                factory = CIO,
                environment =
                    applicationEngineEnvironment {
                        config = HoconApplicationConfig(context.config)
                        module {
                            restModule(
                                sikkerLogg,
                                withMetrics = true,
                                additionalMetrics =
                                    listOf(
                                        JvmGcMetrics(),
                                        JvmThreadMetrics(),
                                    ),
                            ) {
                                trygdetid(trygdetidService, behandlingKlient)
                                trygdetidV2(trygdetidService, behandlingKlient)
                                avtale(avtaleService, behandlingKlient)
                                kodeverk(kodeverkService)
                            }
                        }
                        connector { port = properties.httpPort }
                    },
            )
        }

    fun run() =
        with(context) {
            dataSource.migrate()
            setReady()
            engine.start(true)
        }
}
