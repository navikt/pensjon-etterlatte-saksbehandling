package no.nav.etterlatte.klage

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.etterlatte.klage.modell.KabalOversendelse
import no.nav.etterlatte.libs.common.toJson

interface KabalKlient {
    suspend fun sendTilKabal(kabalOversendelse: KabalOversendelse)
}

class KabalKlientImpl(private val client: HttpClient, private val kabalUrl: String) : KabalKlient {
    override suspend fun sendTilKabal(kabalOversendelse: KabalOversendelse) {
        val response =
            client.post("$kabalUrl/api/oversendelse/v3/sak") {
                contentType(ContentType.Application.Json)
                setBody(kabalOversendelse)
            }

        if (!response.status.isSuccess()) {
            throw KabalKlientException(response.status, kabalOversendelse)
        }
    }
}

class KabalKlientException(httpStatusCode: HttpStatusCode, kabalOversendelse: KabalOversendelse) :
    Exception(
        "Fikk en feil mot Kabal-api ($httpStatusCode) i oversending av klage med id: ${kabalOversendelse.fagSak.fagsakId}. " +
            "Oversendelse: ${kabalOversendelse.toJson()}",
    )
