package no.nav.etterlatte

import institusjonsopphold.KafkaOppholdHendelse
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.person.maskerFnr
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory

class BehandlingKlient(val behandlingHttpClient: HttpClient) {
    private val logger = LoggerFactory.getLogger(this.javaClass.name)

    fun haandterHendelse(record: ConsumerRecord<String, KafkaOppholdHendelse>) {
        logger.debug(
            "Behandler institusjonsopphold record med id: {}, partition {}, offset: {}",
            record.key(),
            record.partition(),
            record.offset()
        )
        val hendelseId = record.key()
        val oppholdHendelse = record.value()
        logger.info(
            "Haandterer institusjonsopphold hendelse for fnr maskert " +
                "${oppholdHendelse.norskident.maskerFnr()} hendelseId: $hendelseId"
        )
        postTilBehandling(oppholdHendelse = oppholdHendelse)
    }

    fun postTilBehandling(oppholdHendelse: KafkaOppholdHendelse) = runBlocking {
        behandlingHttpClient.post(
            "http://etterlatte-behandling/institusjonsopphold"
        ) {
            contentType(ContentType.Application.Json)
            setBody(oppholdHendelse)
        }
    }
}