package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.Application
import no.nav.etterlatte.kafka.startLytting
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServerUtenRest
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.samordning.ApplicationContext
import no.nav.etterlatte.samordning.FAGOMRADE_OMS
import no.nav.etterlatte.samordning.SAKSTYPE_OMS
import no.nav.etterlatte.samordning.SamordningVedtakHendelse
import org.slf4j.LoggerFactory
import kotlin.concurrent.timer

fun main() {
    Server(ApplicationContext()).run()
}

class Server(
    private val context: ApplicationContext,
) {
    private val engine =
        initEmbeddedServerUtenRest(
            httpPort = context.httpPort,
            applicationConfig = ConfigFactory.load(),
        )

    fun run() {
        startLytting(context.konsument, LoggerFactory.getLogger(Application::class.java))
        setReady()
            .also {
                if (context.isProd) {
                    context.handler.logger.info("isProd=true, enabling FAGSYSTEM-335360")

                    timer(
                        name = "fake-tp-svar",
                        daemon = false,
                        initialDelay = 60_000L,
                        period = 0,
                        action = {
                            context.handler.logger.info("Behandler spesialtilfelle FAGSYSTEM-335360")
                            context.handler.handleSamordningHendelse(
                                SamordningVedtakHendelse().apply {
                                    fagomrade = FAGOMRADE_OMS
                                    artTypeKode = SAKSTYPE_OMS
                                    vedtakId = 32136L
                                },
                            )
                        },
                    )
                } else {
                    context.handler.logger.info("isProd=false, skipping FAGSYSTEM-335360")
                }
            }.also { engine.start(true) }
    }
}
