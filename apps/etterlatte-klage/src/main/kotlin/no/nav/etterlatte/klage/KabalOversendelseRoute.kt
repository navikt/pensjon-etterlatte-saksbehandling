package no.nav.etterlatte.klage

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.etterlatte.libs.common.behandling.KlageOversendelseDto
import no.nav.etterlatte.libs.ktor.route.kunSystembruker
import no.nav.etterlatte.libs.ktor.route.medBody
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger(ApplicationContext::class.java)

fun Route.kabalOvesendelseRoute(kabalOversendelseService: KabalOversendelseService) {
    route("api") {
        route("send-klage") {
            post {
                kunSystembruker {
                    medBody<KlageOversendelseDto> {
                        kabalOversendelseService.sendTilKabal(it.klage, it.ekstraData)
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }

        route("temp-test") {
            post {
                try {
                    doit()
                } catch (err: Throwable) {
                    sikkerLogg.error("Feilet", err)
                    logger.error("Feilet, vanlig logger", err)
                }
                logger.info("Normal")
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

private fun doit() {
    if (1 == 1) throw RuntimeException("fds")
}
