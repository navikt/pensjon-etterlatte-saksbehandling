package no.nav.etterlatte.tilbakekreving.klienter

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekreving.SakId
import org.slf4j.LoggerFactory

class BehandlingKlient(
    private val url: String,
    private val httpClient: HttpClient,
) {
    private val logger = LoggerFactory.getLogger(BehandlingKlient::class.java)

    suspend fun opprettTilbakekreving(
        sakId: SakId,
        kravgrunnlag: Kravgrunnlag,
    ) {
        logger.info("Oppretter tilbakekreving for ${sakId.value} og kravgrunnlag ${kravgrunnlag.kravgrunnlagId.value}")
        try {
            httpClient.post("$url/tilbakekreving/${sakId.value}") {
                contentType(ContentType.Application.Json)
                setBody(kravgrunnlag)
            }
        } catch (e: Exception) {
            throw Exception(
                "Klarte ikke å opprette tilbakekreving i sak ${sakId.value} og " +
                    "kravgrunnlag ${kravgrunnlag.kravgrunnlagId.value}",
                e,
            )
        }
    }

    suspend fun endreOppgaveStatusForTilbakekreving(
        sakId: SakId,
        paaVent: Boolean,
    ) {
        logger.info("Setter tilbakekreving på vent i sak ${sakId.value}")
        try {
            httpClient.put("$url/tilbakekreving/${sakId.value}/oppgave-status") {
                contentType(ContentType.Application.Json)
                setBody(OppgaveStatusRequest(paaVent))
            }
        } catch (e: Exception) {
            throw Exception("Klarte ikke å sette tilbakekreving for sak ${sakId.value} på vent", e)
        }
    }

    suspend fun avbrytTilbakekreving(
        sakId: SakId,
        merknad: String,
    ) {
        logger.info("Avbryter tilbakekreving i sak ${sakId.value}")
        try {
            httpClient.put("$url/tilbakekreving/${sakId.value}/avbryt") {
                contentType(ContentType.Application.Json)
                setBody(AvbrytRequest(merknad))
            }
        } catch (e: Exception) {
            throw Exception("Klarte ikke å avbryte tilbakekreving for sak $sakId", e)
        }
    }
}

data class OppgaveStatusRequest(
    val paaVent: Boolean,
)

data class AvbrytRequest(
    val merknad: String,
)
