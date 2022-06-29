package no.nav.etterlatte.behandling

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.*
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post
import io.ktor.routing.route
import no.nav.etterlatte.getAccessToken
import no.nav.etterlatte.libs.common.objectMapper
import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger("no.nav.etterlatte.behandling.VedtakRoute")
fun Route.vedtakRoute(service: VedtakService) {

    route("fattvedtak") {
        post("{behandlingId}") {
            val behandlingId = call.parameters["behandlingId"]
            if (behandlingId == null) {
                call.response.status(HttpStatusCode(400, "Bad request"))
                call.respond("Behandlings-id mangler")
            } else {
                call.respond(service.fattVedtak(behandlingId, getAccessToken(call)))
            }
        }
    }
    route("attestervedtak") {
        post("{behandlingId}") {
            val behandlingId = call.parameters["behandlingId"]
            if (behandlingId == null) {
                call.response.status(HttpStatusCode(400, "Bad request"))
                call.respond("Behandlings-id mangler")
            } else {
                call.respond(service.attesterVedtak(behandlingId, getAccessToken(call)))
            }
        }
    }

    route("underkjennvedtak") {
        post("{behandlingId}") {
            try {
                val behandlingId = call.parameters["behandlingId"]
                logger.info("Skal underkjenne vedtak i behandling $behandlingId")

                val body = call.receiveText()

                logger.info("Underkjennes fordi $body")

                val bodyValue = objectMapper.readValue<UnderkjennVedtakClientRequest>(body)
                logger.info("Underkjennes fordi $bodyValue")

                if (behandlingId == null) {
                    call.response.status(HttpStatusCode(400, "Bad request"))
                    call.respond("Behandlings-id mangler")
                } else {
                    call.respond(
                        service.underkjennVedtak(
                            behandlingId,
                            bodyValue.valgtBegrunnelse,
                            bodyValue.kommentar,
                            getAccessToken(call)
                        )
                    )
                    logger.info("Underkjenningsendepunkt kalt")
                }
            } catch (ex: Exception) {
                logger.error("underkjenning feilet", ex)
                throw ex
            }
        }

    }

}

data class UnderkjennVedtakClientRequest(val kommentar: String, val valgtBegrunnelse: String)
