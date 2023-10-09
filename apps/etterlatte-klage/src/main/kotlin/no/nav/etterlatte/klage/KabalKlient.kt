package no.nav.etterlatte.klage

import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.klage.modell.KabalOversendelse
import no.nav.etterlatte.libs.common.toJson

interface KabalKlient {
    suspend fun sendTilKabal(kabalOversendelse: KabalOversendelse)
}

class KabalKlientImpl(private val client: HttpClient, private val kabalUrl: String) : KabalKlient {
    override suspend fun sendTilKabal(kabalOversendelse: KabalOversendelse) {
        try {
            client.post("$kabalUrl/api/oversendelse/v3/sak") {
                contentType(ContentType.Application.Json)
                setBody(kabalOversendelse)
            }
        } catch (e: ResponseException) {
            throw KabalKlientException(kabalOversendelse, e)
        }
    }
}

class KabalKlientException(
    kabalOversendelse: KabalOversendelse,
    override val cause: Throwable?,
) :
    Exception(
            "Fikk en feil mot Kabal-api i oversending av klage med id: ${kabalOversendelse.fagsak.fagsakId}. " +
                "Oversendelse: ${kabalOversendelse.toJson()}",
        )
