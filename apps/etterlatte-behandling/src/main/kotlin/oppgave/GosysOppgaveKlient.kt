package no.nav.etterlatte.oppgave

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
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

data class GosysOppgaver(val antallTreffTotalt: Int, val oppgaver: List<GosysApiOppgave>)

data class GosysApiOppgave(
    val id: Long,
    val versjon: Long,
    val tema: String,
    val behandlingstema: String,
    val oppgavetype: String,
    val opprettetTidspunkt: Tidspunkt,
    val tildeltEnhetsnr: String,
    val tilordnetRessurs: String?,
    val aktoerId: String,
    val beskrivelse: String,
    val status: String,
    val fristFerdigstillelse: LocalDate,
)

data class GosysEndreSaksbehandlerRequest(
    val versjon: Long,
    val tilordnetRessurs: String,
)

data class GosysEndreFristRequest(
    val versjon: Long,
    val fristFerdigstillelse: LocalDate,
)

interface GosysOppgaveKlient {
    suspend fun hentOppgaver(
        enhetsnr: String? = null,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysOppgaver

    suspend fun tildelOppgaveTilSaksbehandler(
        oppgaveId: String,
        oppgaveVersjon: Long,
        tildeles: String,
        brukerTokenInfo: BrukerTokenInfo,
    )

    suspend fun endreFrist(
        oppgaveId: String,
        oppgaveVersjon: Long,
        nyFrist: LocalDate,
        brukerTokenInfo: BrukerTokenInfo,
    )

    suspend fun hentOppgave(
        id: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysApiOppgave
}

class GosysOppgaveKlientImpl(config: Config, httpClient: HttpClient) : GosysOppgaveKlient {
    private val logger = LoggerFactory.getLogger(GosysOppgaveKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("oppgave.client.id")
    private val resourceUrl = config.getString("oppgave.resource.url")

    override suspend fun hentOppgaver(
        enhetsnr: String?,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysOppgaver {
        try {
            logger.info("Henter oppgaver fra Gosys")

            val filters =
                "statuskategori=AAPEN"
                    .plus("&tema=EYB")
                    .plus("&tema=EYO")
                    .plus(enhetsnr?.let { "&tildeltEnhetsnr=$it" } ?: "")
//                .plus("&tilordnetRessurs=${brukerTokenInfo.ident()}")

            return downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/api/v1/oppgaver?$filters",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (e: Exception) {
            logger.error("Noe feilet mot Gosys [ident=${brukerTokenInfo.ident()}]", e)
            throw e
        }
    }

    override suspend fun hentOppgave(
        id: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysApiOppgave {
        return downstreamResourceClient
            .get(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/v1/oppgaver/$id",
                    ),
                brukerTokenInfo = brukerTokenInfo,
            )
            .mapBoth(
                success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                failure = { errorResponse ->
                    logger.error("Feil ved henting av Gosys oppgave med id=$id", errorResponse.throwable)
                    throw errorResponse
                },
            )
    }

    override suspend fun tildelOppgaveTilSaksbehandler(
        oppgaveId: String,
        oppgaveVersjon: Long,
        tildeles: String,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        try {
            logger.info("Tilordner oppgave $oppgaveId til saksbehandler $tildeles")

            patchOppgave(
                oppgaveId,
                brukerTokenInfo,
                body = GosysEndreSaksbehandlerRequest(oppgaveVersjon, tildeles),
            )
        } catch (e: Exception) {
            logger.error("Noe feilet mot Gosys, ident=${brukerTokenInfo.ident()}]", e)
            throw e
        }
    }

    override suspend fun endreFrist(
        oppgaveId: String,
        oppgaveVersjon: Long,
        nyFrist: LocalDate,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        try {
            logger.info("Endrer frist på oppgave $oppgaveId til $nyFrist")

            patchOppgave(
                oppgaveId,
                brukerTokenInfo,
                body = GosysEndreFristRequest(oppgaveVersjon, nyFrist),
            )
        } catch (e: Exception) {
            logger.error("Noe feilet mot Gosys, ident=${brukerTokenInfo.ident()}]", e)
            throw e
        }
    }

    private suspend fun patchOppgave(
        oppgaveId: String,
        brukerTokenInfo: BrukerTokenInfo,
        body: Any,
    ) {
        downstreamResourceClient
            .patch(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/v1/oppgaver/$oppgaveId",
                    ),
                brukerTokenInfo = brukerTokenInfo,
                patchBody = objectMapper.writeValueAsString(body),
            )
            .mapError { errorResponse -> throw errorResponse }
    }
}
