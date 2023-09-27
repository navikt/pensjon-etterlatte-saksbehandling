package no.nav.etterlatte.tilbakekreving.vedtak

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.tilbakekreving.VedtakId
import no.nav.etterlatte.libs.common.toJson
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakRequest
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakResponse
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsvedtakDto
import org.slf4j.LoggerFactory

// TODO Modellen her utvides når vi kommer så langt
data class Tilbakekrevingsvedtak(val vedtakId: VedtakId)

class TilbakekrevingKlient(private val url: String, private val httpClient: HttpClient) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val sikkerLogg = sikkerlogger()

    fun sendTilbakekrevingsvedtak(tilbakekrevingsvedtak: Tilbakekrevingsvedtak) {
        logger.info("Sender tilbakekrevingsvedtak ${tilbakekrevingsvedtak.vedtakId} til tilbakekrevingskomponenten")
        val request = toTilbakekrevingsvedtakRequest(tilbakekrevingsvedtak)

        val response =
            runBlocking {
                val httpResponse =
                    httpClient.post("$url/tilbakekreving/tilbakekrevingsvedtak") {
                        contentType(ContentType.Application.Json)
                        setBody(request.toJson())
                    }

                httpResponse.body<TilbakekrevingsvedtakResponse>()
            }

        return kontrollerResponse(response)
    }

    private fun toTilbakekrevingsvedtakRequest(tilbakekrevingsvedtak: Tilbakekrevingsvedtak): TilbakekrevingsvedtakRequest {
        return TilbakekrevingsvedtakRequest().apply {
            setTilbakekrevingsvedtak(
                TilbakekrevingsvedtakDto().apply {
                    vedtakId = tilbakekrevingsvedtak.vedtakId.value.toBigInteger()
                    // TODO utvid denne
                },
            )
        }
    }

    private fun kontrollerResponse(response: TilbakekrevingsvedtakResponse) {
        return when (val alvorlighetsgrad = Alvorlighetsgrad.fromString(response.mmel.alvorlighetsgrad)) {
            Alvorlighetsgrad.OK,
            Alvorlighetsgrad.OK_MED_VARSEL,
            -> Unit
            Alvorlighetsgrad.ALVORLIG_FEIL,
            Alvorlighetsgrad.SQL_FEIL,
            -> {
                val err = "Tilbakekrevingsvedtak feilet med alvorlighetsgrad $alvorlighetsgrad"
                sikkerLogg.error(err, kv("response", response.toJson()))
                throw Exception(err)
            }
        }
    }

    enum class Alvorlighetsgrad(val value: String) {
        OK("00"),

        /** En varselmelding følger med */
        OK_MED_VARSEL("04"),

        /** Alvorlig feil som logges og stopper behandling av aktuelt tilfelle*/
        ALVORLIG_FEIL("08"),
        SQL_FEIL("12"),
        ;

        override fun toString() = value

        companion object {
            fun fromString(string: String): Alvorlighetsgrad {
                return enumValues<Alvorlighetsgrad>().first { it.value == string }
            }
        }
    }
}
