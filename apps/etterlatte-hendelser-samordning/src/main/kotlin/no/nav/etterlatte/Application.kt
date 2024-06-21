package no.nav.etterlatte

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.Application
import no.nav.etterlatte.kafka.startLytting
import no.nav.etterlatte.libs.ktor.initialisering.initEmbeddedServerUtenRest
import no.nav.etterlatte.libs.ktor.route.logger
import no.nav.etterlatte.libs.ktor.setReady
import no.nav.etterlatte.samordning.ApplicationContext
import no.nav.etterlatte.samordning.FAGOMRADE_OMS
import no.nav.etterlatte.samordning.SAKSTYPE_OMS
import no.nav.etterlatte.samordning.SamordningVedtakHendelse
import org.slf4j.LoggerFactory
import java.util.Timer
import java.util.TimerTask

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
        setReady().also { engine.start(true) }

        if (context.isProd) {
            Timer("fake-tp-svar").schedule(
                object : TimerTask() {
                    override fun run() {
                        context.handler.logger.info("Behandler spesialtilfelle FAGSYSTEM-335360")
                        context.handler.handleSamordningHendelse(
                            SamordningVedtakHendelse().apply {
                                fagomrade = FAGOMRADE_OMS
                                artTypeKode = SAKSTYPE_OMS
                                vedtakId = 32136L
                            },
                        )
                    }
                },
                60_000L,
            )
        }
    }
}
