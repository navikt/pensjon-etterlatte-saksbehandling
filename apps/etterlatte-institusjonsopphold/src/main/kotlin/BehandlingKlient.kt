package no.nav.etterlatte

import institusjonsopphold.InstitusjonsoppholdHendelseBeriket
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.kafka.KafkaOppholdHendelse
import no.nav.etterlatte.libs.common.person.maskerFnr
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory

class BehandlingKlient(val behandlingHttpClient: HttpClient, val institusjonsoppholdKlient: InstitusjonsoppholdKlient) {
    private val logger = LoggerFactory.getLogger(this.javaClass.name)

    fun haandterHendelse(record: ConsumerRecord<String, KafkaOppholdHendelse>) {
        logger.debug(
            "Behandler institusjonsopphold record med id: {}, partition {}, offset: {}",
            record.key(),
            record.partition(),
            record.offset()
        )
        val oppholdHendelse = record.value()
        logger.info(
            "Haandterer institusjonsopphold hendelse for fnr maskert " +
                "${oppholdHendelse.norskident.maskerFnr()} hendelseId: ${oppholdHendelse.hendelseId}"
        )

        val opphold: Institusjonsopphold = institusjonsoppholdKlient.hentDataForHendelse(oppholdHendelse.oppholdId)

        postTilBehandling(
            oppholdHendelse = InstitusjonsoppholdHendelseBeriket(
                oppholdId = oppholdHendelse.oppholdId,
                hendelseId = oppholdHendelse.hendelseId,
                norskident = oppholdHendelse.norskident,
                institusjonsoppholdsType = oppholdHendelse.institusjonsoppholdsType,
                institusjonsoppholdKilde = oppholdHendelse.institusjonsoppholdKilde,
                institusjonsnavn = opphold.institusjonsnavn,
                institusjonsType = opphold.institusjonstype!!,
                startdato = opphold.startdato,
                faktiskSluttdato = opphold.faktiskSluttdato,
                forventetSluttdato = opphold.forventetSluttdato
            )
        )
    }

    fun postTilBehandling(oppholdHendelse: InstitusjonsoppholdHendelseBeriket) = runBlocking {
        behandlingHttpClient.post(
            "http://etterlatte-behandling/grunnlagsendringshendelse/institusjonsopphold"
        ) {
            contentType(ContentType.Application.Json)
            setBody(oppholdHendelse)
        }
    }
}