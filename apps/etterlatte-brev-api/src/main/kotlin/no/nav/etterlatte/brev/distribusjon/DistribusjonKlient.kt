package no.nav.etterlatte.brev.distribusjon

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.TimeoutForespoerselException
import org.slf4j.LoggerFactory

class DistribusjonKlient(
    val client: HttpClient,
    val url: String,
) {
    val logger = LoggerFactory.getLogger(DistribusjonKlient::class.java)

    internal suspend fun distribuerJournalpost(request: DistribuerJournalpostRequest): DistribuerJournalpostResponse =
        try {
            client
                .post("$url/distribuerjournalpost") {
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.let {
                    when (it.status) {
                        HttpStatusCode.OK -> it.body()
                        HttpStatusCode.Conflict -> it.body()
                        else -> {
                            logger.error("Fikk statuskode ${it.status} fra dokdist: ${it.bodyAsText()}")

                            throw ForespoerselException(
                                status = it.status.value,
                                code = "UKJENT_FEIL_DOKDIST",
                                detail = "Ukjent respons fra dokumentdistribusjon",
                                meta =
                                    mapOf(
                                        "journalpostId" to request.journalpostId,
                                    ),
                                cause = ResponseException(it, "Ukjent feil fra dokdist"),
                            )
                        }
                    }
                }
        } catch (ex: SocketTimeoutException) {
            logger.warn("Timeout mot dokdist (journalpostId=${request.journalpostId})", ex)

            throw TimeoutForespoerselException(
                code = "TIMEOUT_DOKDIST",
                detail = "Kallet mot distribusjonstjenesten tok for lang tid. Prøv igjen.",
            )
        }
}
