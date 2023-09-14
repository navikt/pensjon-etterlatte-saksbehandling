package no.nav.etterlatte.klage

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.klage.modell.BehandlingEvent
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.toJson
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory

class BehandlingKlient(config: Config, val behandlingHttpClient: HttpClient) {
    private val logger = LoggerFactory.getLogger(this.javaClass.name)

    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")

    fun haandterHendelse(record: ConsumerRecord<String, BehandlingEvent>) {
        logger.debug(
            "Behandler klage-record med id: {}, partition {}, offset: {}",
            record.key(),
            record.partition(),
            record.offset()
        )
        logger.info("Skal behandle record ${record.toJson()}")
        val klageHendelse = record.value()
        logger.info(
            "Håndterer klagehendelse ${klageHendelse.eventId}"
        )

        if (klageHendelse.kilde == Vedtaksloesning.GJENNY.name) {
            postTilBehandling(klageHendelse)
        }
    }

    private fun postTilBehandling(klageHendelse: BehandlingEvent) = runBlocking {
        behandlingHttpClient.post(
            "$resourceUrl/klage"
        ) {
            contentType(ContentType.Application.Json)
            setBody(klageHendelse)
        }
    }
}