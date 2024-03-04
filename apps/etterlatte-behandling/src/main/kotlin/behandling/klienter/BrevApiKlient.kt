package no.nav.etterlatte.behandling.klienter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.behandling.objectMapper
import no.nav.etterlatte.libs.common.behandling.Mottaker
import no.nav.etterlatte.libs.common.brev.BestillingsIdDto
import no.nav.etterlatte.libs.common.brev.JournalpostIdDto
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

interface BrevApiKlient {
    suspend fun opprettKlageInnstillingsbrevISak(
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): OpprettetBrevDto

    suspend fun opprettVedtaksbrev(
        behandlingId: UUID,
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): OpprettetBrevDto

    suspend fun ferdigstillBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    )

    /**
     * @return journalpostId til brevets journalpost
     */
    suspend fun journalfoerBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): JournalpostIdDto

    /**
     * @return bestillingsId for distribusjonen
     */
    suspend fun distribuerBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): BestillingsIdDto

    suspend fun hentBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): OpprettetBrevDto

    suspend fun slettVedtaksbrev(
        klageId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    )
}

class BrevApiKlientObo(config: Config, client: HttpClient) : BrevApiKlient {
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, client)

    private val clientId = config.getString("brev-api.client.id")
    private val resourceUrl = config.getString("brev-api.resource.url")

    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun opprettKlageInnstillingsbrevISak(
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): OpprettetBrevDto {
        val opprettetBrev: OpprettetBrevDto =
            post(
                url = "$resourceUrl/api/brev/sak/$sakId",
                onSuccess = { resource ->
                    resource.response?.let { objectMapper.readValue(it.toJson()) }
                        ?: throw RuntimeException("Fikk ikke en riktig respons fra oppretting av brev")
                },
                brukerTokenInfo = brukerTokenInfo,
            )
        try {
            settTittelForBrev(sakId, opprettetBrev.id, "Oversendelsesbrev til NAV Klageinstans", brukerTokenInfo)
        } catch (e: Exception) {
            logger.warn(
                "Fikk ikke satt tittel på opprettet innstillingsbrev med id=${opprettetBrev.id} " +
                    "for klage i sak med id=$sakId på grunn av feil. Saksbehandler kan endre i frontend som " +
                    "workaround.",
                e,
            )
        }
        return opprettetBrev
    }

    override suspend fun opprettVedtaksbrev(
        behandlingId: UUID,
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): OpprettetBrevDto {
        return post(
            url = "$resourceUrl/api/brev/behandling/$behandlingId/vedtak?sakId=$sakId",
            onSuccess = { resource -> deserialize(resource.response!!.toJson()) },
            brukerTokenInfo = brukerTokenInfo,
        )
    }

    override suspend fun ferdigstillBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        post(
            url = "$resourceUrl/api/brev/$brevId/ferdigstill?sakId=$sakId",
            onSuccess = { },
            brukerTokenInfo = brukerTokenInfo,
        )
    }

    override suspend fun journalfoerBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): JournalpostIdDto {
        return post(
            url = "$resourceUrl/api/brev/$brevId/journalfoer?sakId=$sakId",
            onSuccess = { resource -> deserialize(resource.response!!.toJson()) },
            brukerTokenInfo = brukerTokenInfo,
        )
    }

    override suspend fun distribuerBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): BestillingsIdDto {
        return post(
            url = "$resourceUrl/api/brev/$brevId/distribuer?sakId=$sakId",
            onSuccess = { resource -> deserialize(resource.response!!.toJson()) },
            brukerTokenInfo = brukerTokenInfo,
        )
    }

    override suspend fun hentBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): OpprettetBrevDto {
        return get(
            url = "$resourceUrl/api/brev/$brevId?sakId=$sakId",
            onSuccess = { resource -> deserialize(resource.response!!.toJson()) },
            brukerTokenInfo = brukerTokenInfo,
        )
    }

    override suspend fun slettVedtaksbrev(
        klageId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        downstreamResourceClient.delete(
            resource = Resource(clientId = clientId, url = "$resourceUrl/api/brev/behandling/$klageId/vedtak"),
            brukerTokenInfo = brukerTokenInfo,
            postBody = "",
        ).mapError { error -> throw error }
    }

    private suspend fun settTittelForBrev(
        sakId: Long,
        brevId: Long,
        tittel: String,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        post(
            url = "$resourceUrl/api/brev/$brevId/tittel?sakId=$sakId",
            postBody = OppdaterTittelRequest(tittel),
            onSuccess = {},
            brukerTokenInfo = brukerTokenInfo,
        )
    }

    private suspend fun <T> post(
        url: String,
        postBody: Any = Unit,
        onSuccess: (Resource) -> T,
        brukerTokenInfo: BrukerTokenInfo,
    ): T {
        return downstreamResourceClient.post(
            resource = Resource(clientId = clientId, url = url),
            brukerTokenInfo = brukerTokenInfo,
            postBody = postBody,
        ).mapBoth(
            success = onSuccess,
            failure = { errorResponse -> throw errorResponse },
        )
    }

    private suspend fun <T> get(
        url: String,
        onSuccess: (Resource) -> T,
        brukerTokenInfo: BrukerTokenInfo,
    ): T {
        return downstreamResourceClient.get(
            resource = Resource(clientId = clientId, url = url),
            brukerTokenInfo = brukerTokenInfo,
        ).mapBoth(
            success = onSuccess,
            failure = { throwableErrorMessage -> throw throwableErrorMessage },
        )
    }
}

data class OppdaterTittelRequest(
    val tittel: String,
)

enum class BrevStatus {
    OPPRETTET,
    OPPDATERT,
    FERDIGSTILT,
    JOURNALFOERT,
    DISTRIBUERT,
    SLETTET,
    ;

    fun ikkeFerdigstilt(): Boolean {
        return this in listOf(OPPRETTET, OPPDATERT, SLETTET)
    }

    fun ikkeJournalfoert(): Boolean {
        return this in listOf(OPPRETTET, OPPDATERT, FERDIGSTILT, SLETTET)
    }

    fun ikkeDistribuert(): Boolean {
        return this != DISTRIBUERT
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpprettetBrevDto(
    val id: Long,
    val mottaker: Mottaker,
    val status: BrevStatus,
    val journalpostId: String?,
    val bestillingsID: String?,
)
