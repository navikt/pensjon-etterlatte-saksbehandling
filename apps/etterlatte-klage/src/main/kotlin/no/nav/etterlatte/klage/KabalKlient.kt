package no.nav.etterlatte.klage

import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.etterlatte.klage.modell.KabalOversendelse
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface KabalKlient {
    suspend fun sendTilKabal(kabalOversendelse: KabalOversendelse)
}

class KabalKlientImpl(
    private val client: HttpClient,
    private val kabalUrl: String,
) : KabalKlient {
    private val logger: Logger = LoggerFactory.getLogger(KabalKlient::class.java)

    override suspend fun sendTilKabal(kabalOversendelse: KabalOversendelse) {
        try {
            client.post("$kabalUrl/api/oversendelse/v3/sak") {
                contentType(ContentType.Application.Json)
                setBody(kabalOversendelse)
            }
        } catch (e: ResponseException) {
            val body = e.response.bodyAsText()
            if (e.response.status == HttpStatusCode.Conflict) {
                logger.warn(
                    "Fikk conflict i ferdigstilling av klagen med id=${kabalOversendelse.fagsak.fagsakId}" +
                        " fra Kabal, siden klagen allerede er oversendt. Ignorerer feilen. Full response fra Kabal" +
                        "var $body",
                )
            } else {
                throw KabalKlientException(kabalOversendelse, body, e)
            }
        }
    }
}

class KabalKlientException(
    kabalOversendelse: KabalOversendelse,
    responseBody: String,
    override val cause: ResponseException,
) : Exception(
        "Fikk en feil mot Kabal-api i oversending av klage med id: ${kabalOversendelse.fagsak.fagsakId}. " +
            "Oversendelse hadde feil: $responseBody",
    )
