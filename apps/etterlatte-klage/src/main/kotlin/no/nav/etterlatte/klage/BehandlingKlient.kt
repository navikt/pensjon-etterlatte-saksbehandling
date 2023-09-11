package no.nav.etterlatte.klage

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.klage.modell.BehandlingEvent
import no.nav.etterlatte.libs.common.Vedtaksloesning
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory

class BehandlingKlient(val behandlingHttpClient: HttpClient) {
    private val logger = LoggerFactory.getLogger(this.javaClass.name)

    fun haandterHendelse(record: ConsumerRecord<String, BehandlingEvent>) {
        logger.debug(
            "Behandler institusjonsopphold record med id: {}, partition {}, offset: {}",
            record.key(),
            record.partition(),
            record.offset()
        )
        val klageHendelse = record.value()
        logger.info(
            "Håndterer klagehendelse ${klageHendelse.eventId}"
        )

        if (klageHendelse.kilde == Vedtaksloesning.GJENNY.name) {
            postTilBehandling(klageHendelse)
        }
    }

    fun postTilBehandling(klageHendelse: BehandlingEvent) = runBlocking {
        behandlingHttpClient.post(
            "http://etterlatte-behandling/klage"
        ) {
            contentType(ContentType.Application.Json)
            setBody(klageHendelse)
        }
    }
}