package no.nav.etterlatte.oppgaveGosys

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.network.sockets.SocketTimeoutException
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.LocalDate

data class GosysOppgaver(val antallTreffTotalt: Int, val oppgaver: List<GosysApiOppgave>)

data class GosysApiOppgave(
    val id: Long,
    val versjon: Long,
    val tema: String,
    val behandlingstema: String? = null,
    val oppgavetype: String,
    val journalpostId: String?,
    val opprettetTidspunkt: Tidspunkt,
    val tildeltEnhetsnr: String,
    val tilordnetRessurs: String?,
    val aktoerId: String?,
    val beskrivelse: String?,
    val status: String,
    val fristFerdigstillelse: LocalDate? = null,
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
        saksbehandler: String?,
        tema: List<String>,
        enhetsnr: String? = null,
        harTildeling: Boolean?,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysOppgaver

    suspend fun hentJournalfoeringsoppgave(
        journalpostId: String,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysOppgaver

    suspend fun tildelOppgaveTilSaksbehandler(
        oppgaveId: String,
        oppgaveVersjon: Long,
        tildeles: String,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysApiOppgave

    suspend fun endreFrist(
        oppgaveId: String,
        oppgaveVersjon: Long,
        nyFrist: LocalDate,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysApiOppgave

    suspend fun hentOppgave(
        id: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysApiOppgave

    suspend fun ferdigstill(
        id: String,
        oppgaveVersjon: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysApiOppgave

    suspend fun feilregistrer(
        id: String,
        request: EndreStatusRequest,
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
        saksbehandler: String?,
        tema: List<String>,
        enhetsnr: String?,
        harTildeling: Boolean?,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysOppgaver {
        try {
            logger.info("Henter oppgaver fra Gosys")

            val temaFilter = tema.map { "&tema=$it" }.joinToString(separator = "")
            val filters =
                "statuskategori=AAPEN"
                    .plus(temaFilter)
                    .plus("&limit=1000")
                    .plus(enhetsnr?.let { "&tildeltEnhetsnr=$it" } ?: "")
                    .plus(saksbehandler?.let { "&tilordnetRessurs=$it" } ?: "")
                    .plus(harTildeling?.let { "&tildeltRessurs=$it" } ?: "")

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
        } catch (e: SocketTimeoutException) {
            throw GosysTimeout()
        } catch (e: Exception) {
            logger.error("Feil ved henting av oppgaver fra Gosys", e)
            throw e
        }
    }

    override suspend fun hentJournalfoeringsoppgave(
        journalpostId: String,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysOppgaver {
        try {
            logger.info("Henter journalføringsoppgaver for journalpostId=$journalpostId fra Gosys")

            val filters = "statuskategori=AAPEN&journalpostId=$journalpostId"

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
                    success = { resource -> objectMapper.readValue(resource.response.toString()) },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (e: SocketTimeoutException) {
            throw GosysTimeout()
        } catch (e: Exception) {
            logger.error("Feil ved henting av oppgaver fra Gosys", e)
            throw e
        }
    }

    override suspend fun hentOppgave(
        id: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysApiOppgave {
        try {
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
                    success = { resource -> deserialize(resource.response.toString()) },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (e: SocketTimeoutException) {
            throw GosysTimeout()
        } catch (e: Exception) {
            logger.error("Feil ved henting av oppgave med id=$id fra Gosys", e)
            throw e
        }
    }

    override suspend fun ferdigstill(
        id: String,
        oppgaveVersjon: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysApiOppgave {
        logger.info("Ferdigstiller Gosys-oppgave med id=$id")

        return patchOppgave(
            id,
            brukerTokenInfo,
            body = EndreStatusRequest(oppgaveVersjon.toString(), "FERDIGSTILT"),
        )
    }

    override suspend fun feilregistrer(
        id: String,
        request: EndreStatusRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysApiOppgave {
        logger.info("Feilregistrerer Gosys-oppgave med id=$id")

        return patchOppgave(
            id,
            brukerTokenInfo,
            body = request,
        )
    }

    override suspend fun tildelOppgaveTilSaksbehandler(
        oppgaveId: String,
        oppgaveVersjon: Long,
        tildeles: String,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysApiOppgave {
        logger.info("Tilordner oppgave $oppgaveId til saksbehandler $tildeles")

        return patchOppgave(
            oppgaveId,
            brukerTokenInfo,
            body = GosysEndreSaksbehandlerRequest(oppgaveVersjon, tildeles),
        )
    }

    override suspend fun endreFrist(
        oppgaveId: String,
        oppgaveVersjon: Long,
        nyFrist: LocalDate,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysApiOppgave {
        logger.info("Endrer frist på oppgave $oppgaveId til $nyFrist")

        return patchOppgave(
            oppgaveId,
            brukerTokenInfo,
            body = GosysEndreFristRequest(oppgaveVersjon, nyFrist),
        )
    }

    private suspend fun patchOppgave(
        oppgaveId: String,
        brukerTokenInfo: BrukerTokenInfo,
        body: Any,
    ): GosysApiOppgave {
        try {
            return downstreamResourceClient
                .patch(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/api/v1/oppgaver/$oppgaveId",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                    patchBody = objectMapper.writeValueAsString(body),
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue<GosysApiOppgave>(it.toString()) } },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (re: ResponseException) {
            if (re.response.status == HttpStatusCode.Conflict) {
                throw GosysKonfliktException(re.response.bodyAsText())
            } else {
                throw re
            }
        } catch (e: SocketTimeoutException) {
            throw GosysTimeout()
        } catch (e: Exception) {
            logger.error("Ukjent feil oppsto ved patching av oppgave=$oppgaveId (se sikkerlogg for body)", e)
            sikkerlogger().error("Ukjent feil oppsto ved patching av oppgave=$oppgaveId: \n${body.toJson()}")
            throw e
        }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EndreStatusRequest(
    val versjon: String,
    val status: String,
    val beskrivelse: String? = null,
)

class GosysTimeout : ForespoerselException(
    status = HttpStatusCode.RequestTimeout.value,
    code = "GOSYS_TIMEOUT",
    detail = "Henting av oppgave(er) fra Gosys tok for lang tid. Prøv igjen senere.",
)

class GosysKonfliktException(detail: String) : ForespoerselException(
    status = HttpStatusCode.Conflict.value,
    code = "GOSYS_OPTIMISTISK_LAAS",
    detail = detail,
)
