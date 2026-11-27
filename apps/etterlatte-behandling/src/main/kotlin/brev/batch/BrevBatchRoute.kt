package no.nav.etterlatte.brev.batch

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.readAvailable
import no.nav.etterlatte.brev.BrevBatchRequest
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.ktor.token.brukerTokenInfo
import java.io.ByteArrayOutputStream
import java.util.UUID

fun Route.brevBatchRoute(service: BrevBatchService) {
    route("/api/brev/batcher") {
        post {
            service.autoriser(brukerTokenInfo)
            val noekkel = uuid(call.request.headers["Idempotency-Key"])
            val channel = call.receiveChannel()
            val bytes = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val antall = channel.readAvailable(buffer, 0, buffer.size)
                if (antall == -1) break
                if (bytes.size() + antall > BrevBatchService.MAKS_REQUEST_BYTES) {
                    channel.cancel(null)
                    throw ForespoerselException(413, "BREV_BATCH_STOERRELSE", "Requesten er for stor")
                }
                bytes.write(buffer, 0, antall)
            }
            val request =
                try {
                    deserialize<BrevBatchRequest>(bytes.toByteArray().decodeToString(throwOnInvalidSequence = true))
                } catch (_: Exception) {
                    throw UgyldigForespoerselException("BREV_BATCH_REQUEST", "Ugyldig request")
                }
            val batch = service.opprett(brukerTokenInfo, noekkel, request)
            call.response.headers.append("Location", "/api/brev/batcher/${batch.batchId}")
            call.respond(HttpStatusCode.Accepted, batch)
        }
        get("/{batchId}") {
            call.respond(service.hent(brukerTokenInfo, uuid(call.parameters["batchId"])))
        }
        get("/{batchId}/saker") {
            val offset = call.request.queryParameters["offset"]?.let { heltall(it) } ?: 0
            val limit = call.request.queryParameters["limit"]?.let { heltall(it) } ?: 100
            call.respond(service.hentSaker(brukerTokenInfo, uuid(call.parameters["batchId"]), offset, limit))
        }
    }
}

private fun uuid(value: String?): UUID =
    try {
        require(value != null && Regex("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}").matches(value))
        UUID.fromString(value)
    } catch (_: IllegalArgumentException) {
        throw UgyldigForespoerselException("BREV_BATCH_UUID", "Mangler gyldig UUID")
    }

private fun heltall(value: String): Int = value.toIntOrNull() ?: throw UgyldigForespoerselException("BREV_BATCH_SIDE", "Ugyldig paginering")
