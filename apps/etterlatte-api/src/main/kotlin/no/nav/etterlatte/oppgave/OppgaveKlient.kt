package no.nav.etterlatte.oppgave

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.etterlatte.behandling.sak.BehandlingManglendeTilgang
import no.nav.etterlatte.behandling.sak.BehandlingUgyldigForespoersel
import no.nav.etterlatte.libs.common.oppgave.NyOppgaveDto
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.sak.SakId
import org.slf4j.LoggerFactory

class OppgaveKlient(
    config: Config,
    private val httpClient: HttpClient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val behandlingUrl = "${config.getString("behandling.url")}/api"

    suspend fun opprettOppgave(
        sakId: SakId,
        oppgave: NyOppgaveDto,
    ): OppgaveIntern {
        logger.info("Oppretter ny oppgave på sak $sakId")

        return try {
            httpClient
                .post("$behandlingUrl/oppgaver/sak/${sakId.sakId}/opprett") {
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    setBody(oppgave)
                }.body<OppgaveIntern>()
        } catch (e: ClientRequestException) {
            logger.error("Det oppstod en feil ved opprettelse av oppgave")
            when (e.response.status) {
                HttpStatusCode.Unauthorized -> throw BehandlingManglendeTilgang("Behandling: Ikke tilgang")
                HttpStatusCode.BadRequest -> throw BehandlingUgyldigForespoersel("Behandling: Ugyldig forespørsel")
                else -> throw e
            }
        }
    }
}
