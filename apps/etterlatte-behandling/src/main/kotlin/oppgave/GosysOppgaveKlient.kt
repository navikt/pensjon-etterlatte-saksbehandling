package no.nav.etterlatte.oppgave

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

data class GosysOppgaver(val antallTreffTotalt: Int, val oppgaver: List<GosysOppgave>)
data class GosysOppgave(val id: Int, val tildeltEnhetsnr: String, val aktoerId: String, val beskrivelse: String)

interface GosysOppgaveKlient {
    suspend fun hentOppgaver(tema: String, enhetsnr: String, brukerTokenInfo: BrukerTokenInfo): GosysOppgaver
}

class GosysOppgaveKlientImpl(config: Config, httpClient: HttpClient) : GosysOppgaveKlient {

    private val logger = LoggerFactory.getLogger(GrunnlagKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("oppgave.client.id")
    private val resourceUrl = config.getString("oppgave.resource.url")

    override suspend fun hentOppgaver(tema: String, enhetsnr: String, brukerTokenInfo: BrukerTokenInfo): GosysOppgaver {
        try {
            logger.info("Henter oppgaver fra Gosys")

            val ident = brukerTokenInfo.ident()
            val filters = "statuskategori=AAPEN&tema=${tema.uppercase()}&tildeltEnhetsnr=$enhetsnr&tilordnetRessurs=$ident"

            return downstreamResourceClient
                .get(
                    resource = Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/v1/oppgaver?$filters"
                    ),
                    brukerTokenInfo = brukerTokenInfo
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { errorResponse -> throw errorResponse }
                )
        } catch (e: Exception) {
            logger.error("Noe feilet mot Gosys", e)
            throw e
        }
    }
}