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
import no.nav.etterlatte.libs.ktor.PingResult
import no.nav.etterlatte.libs.ktor.Pingable
import no.nav.etterlatte.libs.ktor.ping
import org.slf4j.LoggerFactory

interface KlageKlient : Pingable {
    suspend fun sendKlageTilKabal(
        klage: Klage,
        ekstradataInnstilling: EkstradataInnstilling,
    )
}

class KlageKlientImpl(
    private val client: HttpClient,
    private val url: String,
) : KlageKlient {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun sendKlageTilKabal(
        klage: Klage,
        ekstradataInnstilling: EkstradataInnstilling,
    ) {
        val response =
            client.post("$url/api/send-klage") {
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

    override val serviceName: String
        get() = "Klage klient"
    override val beskrivelse: String
        get() = "Sender klage til kabal via proxy app"
    override val endpoint: String
        get() = this.url

    override suspend fun ping(konsument: String?): PingResult =
        client.ping(
            pingUrl = url.plus("/health/isready"),
            logger = logger,
            serviceName = serviceName,
            beskrivelse = beskrivelse,
        )
}

class KlageOversendelseException(
    klage: Klage,
    statusCode: HttpStatusCode,
) : Exception("Kunne ikke oversende klagen med id=${klage.id} til etterlatte-klage, fikk statuskode=$statusCode")
