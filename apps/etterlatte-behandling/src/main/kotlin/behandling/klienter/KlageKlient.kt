package no.nav.etterlatte.behandling.klienter

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.etterlatte.libs.common.behandling.EkstradataInnstilling
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.KlageOversendelseDto

interface KlageKlient {
    suspend fun sendKlageTilKabal(
        klage: Klage,
        ekstradataInnstilling: EkstradataInnstilling,
    )
}

class KlageKlientImpl(private val httpClient: HttpClient, private val resourceUrl: String) : KlageKlient {
    override suspend fun sendKlageTilKabal(
        klage: Klage,
        ekstradataInnstilling: EkstradataInnstilling,
    ) {
        val response =
            httpClient.post("$resourceUrl/api/send-klage") {
                contentType(ContentType.Application.Json)
                setBody(
                    KlageOversendelseDto(
                        klage = klage,
                        ekstraData = ekstradataInnstilling,
                    ),
                )
            }
        if (!response.status.isSuccess()) {
            throw KlageOversendelseException(klage, response.status)
        }
    }
}

class KlageOversendelseException(klage: Klage, statusCode: HttpStatusCode) :
    Exception("Kunne ikke oversende klagen med id=${klage.id} til etterlatte-klage, fikk statuskode=$statusCode")
