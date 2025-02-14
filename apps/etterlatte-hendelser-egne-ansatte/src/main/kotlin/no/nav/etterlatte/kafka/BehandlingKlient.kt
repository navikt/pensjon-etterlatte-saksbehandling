package no.nav.etterlatte.kafka

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.skjermet.EgenAnsattSkjermet
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory

class BehandlingKlient(
    val behandlingHttpClient: HttpClient,
    val url: String,
) {
    private val logger = LoggerFactory.getLogger(this.javaClass.name)

    fun haandterHendelse(record: ConsumerRecord<String, String>) {
        logger.debug(
            "Behandler skjermet record med id: {}, partition {}, offset: {}",
            record.key(),
            record.partition(),
            record.offset(),
        )
        val skjermet = record.value().toBoolean()
        val fnr = record.key()

        if (Folkeregisteridentifikator.isValid(fnr)) {
            logger.info("Haandterer skjermet hendelse for fnr maskert ${fnr.maskerFnr()}")
            // TODO endre til å poste til behandling når den gamle appen er slått av
            // postTilBehandling(fnr = fnr, skjermet = skjermet)
        } else {
            sikkerlogger().error("Ugyldig fnr. ($fnr) i skjermet hendelse, er skjermet $skjermet")
            logger.error("Ugyldig fnr i skjermet hendelse, er skjermet $skjermet. Se sikkerlogg. ")
        }
    }

    fun postTilBehandling(
        fnr: String,
        skjermet: Boolean,
    ) = runBlocking {
        behandlingHttpClient.post(
            "$url/egenansatt",
        ) {
            contentType(ContentType.Application.Json)
            setBody(
                EgenAnsattSkjermet(
                    fnr = fnr,
                    inntruffet = Tidspunkt.now(),
                    skjermet = skjermet,
                ),
            )
        }
    }
}
