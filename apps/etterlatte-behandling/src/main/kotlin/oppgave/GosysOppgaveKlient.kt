package no.nav.etterlatte.oppgave

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.LocalDate

data class GosysOppgaver(val antallTreffTotalt: Int, val oppgaver: List<GosysOppgave>)

data class GosysOppgave(
    val id: Int,
    val tema: String,
    val behandlingstema: String,
    val oppgavetype: String,
    val opprettetTidspunkt: Tidspunkt,
    val tildeltEnhetsnr: String,
    val tilordnetRessurs: String?,
    val aktoerId: String,
    val beskrivelse: String,
    val status: String,
    val fristFerdigstillelse: LocalDate
)

interface GosysOppgaveKlient {
    suspend fun hentOppgaver(tema: String, enhetsnr: String, brukerTokenInfo: BrukerTokenInfo): GosysOppgaver
}

class GosysOppgaveKlientImpl(config: Config, httpClient: HttpClient) : GosysOppgaveKlient {

    private val logger = LoggerFactory.getLogger(GosysOppgaveKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("oppgave.client.id")
    private val resourceUrl = config.getString("oppgave.resource.url")

    override suspend fun hentOppgaver(tema: String, enhetsnr: String, brukerTokenInfo: BrukerTokenInfo): GosysOppgaver {
        try {
            logger.info("Henter oppgaver fra Gosys")

            val filters = "statuskategori=AAPEN"
                .plus("&tema=PEN")
                .plus("&tema=EYO")
                .plus("&tildeltEnhetsnr=$enhetsnr")
                .plus("&tilordnetRessurs=${brukerTokenInfo.ident()}")

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
            logger.error("Noe feilet mot Gosys [tema=$tema, enhetsnr=$enhetsnr, ident=${brukerTokenInfo.ident()}]", e)
            throw e
        }
    }
}