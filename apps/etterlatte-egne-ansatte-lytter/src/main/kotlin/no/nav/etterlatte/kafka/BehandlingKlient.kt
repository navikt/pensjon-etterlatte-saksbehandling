package no.nav.etterlatte.kafka

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.feilhaandtering.ExceptionResponse
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.pdl.PdlFeilAarsak
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
            postTilBehandling(fnr = fnr, skjermet = skjermet)
        } else {
            sikkerlogger().error("Ugyldig fnr. ($fnr) i skjermet hendelse, er skjermet $skjermet")
            logger.error("Ugyldig fnr i skjermet hendelse, er skjermet $skjermet. Se sikkerlogg. ")
        }
    }

    fun postTilBehandling(
        fnr: String,
        skjermet: Boolean,
    ) = runBlocking {
        try {
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
        } catch (feil: Exception) {
            if (feil !is ResponseException) {
                throw feil
            }

            val pdlExceptionCode =
                try {
                    feil.response.body<ExceptionResponse>().code
                } catch (e: Exception) {
                    logger.warn("Noe rart har skjedd her, vi får feil fra PDL som ikke har en exception response", e)
                    throw feil
                }

            if (pdlExceptionCode == PdlFeilAarsak.FANT_IKKE_PERSON.name) {
                sikkerlogger().error("Klarte ikke slå opp i PDL på person $fnr")
                logger.error("Vi får feil fra PDL, fant ikke person med ident ${fnr.maskerFnr()}", feil)
            } else {
                throw feil
            }
        }
    }
}
