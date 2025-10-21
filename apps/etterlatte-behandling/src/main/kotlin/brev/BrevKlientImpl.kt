package no.nav.etterlatte.brev

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeoutConfig
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.KanFerdigstilleBrevResponse
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.route.SAKID_CALL_PARAMETER
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.time.Duration
import java.util.UUID

interface BrevKlient {
    suspend fun tilbakestillStrukturertBrev(
        brevID: BrevID,
        behandlingId: UUID,
        brevRequest: BrevRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevPayload

    suspend fun ferdigstillStrukturertBrev(
        behandlingId: UUID,
        brevType: Brevtype,
        brukerTokenInfo: BrukerTokenInfo,
    )

    suspend fun ferdigstillJournalfoerStrukturertBrev(
        behandlingId: UUID,
        brevType: Brevtype,
        brukerTokenInfo: BrukerTokenInfo,
    )

    suspend fun kanFerdigstilleBrev(
        brevId: BrevID,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): KanFerdigstilleBrevResponse

    suspend fun genererPdf(
        brevID: BrevID,
        behandlingId: UUID,
        brevRequest: BrevRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): Pdf

    suspend fun opprettStrukturertBrev(
        behandlingId: UUID,
        brevRequest: BrevRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev

    suspend fun hentVedtaksbrev(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ): Brev?

    suspend fun hentBrev(
        sakId: SakId,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev

    suspend fun slettBrev(
        brevSomskalSlettes: BrevID,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    )
}

class BrevKlientImpl(
    config: Config,
    client: HttpClient,
) : BrevKlient {
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, client)

    private val clientId = config.getString("brev-api.client.id")
    private val resourceUrl = config.getString("brev-api.resource.url")

    override suspend fun opprettStrukturertBrev(
        behandlingId: UUID,
        brevRequest: BrevRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev =
        post(
            url = "$resourceUrl/api/brev/strukturert/$behandlingId/",
            onSuccess = { resource -> deserialize(resource.response!!.toJson()) },
            brukerTokenInfo = brukerTokenInfo,
            postBody = brevRequest,
        )

    override suspend fun hentVedtaksbrev(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ): Brev? =
        get(
            url = "$resourceUrl/api/brev/behandling/$behandlingId/vedtak",
            onSuccess = { resource -> resource.response?.let { deserialize(it.toJson()) } },
            brukerTokenInfo = bruker,
        )

    override suspend fun hentBrev(
        sakId: SakId,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev =
        get(
            url = "$resourceUrl/api/brev/$brevId?sakId=${sakId.sakId}",
            onSuccess = { resource -> deserialize(resource.response!!.toJson()) },
            brukerTokenInfo = brukerTokenInfo,
        )

    override suspend fun genererPdf(
        brevID: BrevID,
        behandlingId: UUID,
        brevRequest: BrevRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): Pdf =
        post(
            url = "$resourceUrl/api/brev/strukturert/$behandlingId/pdf?brevId=$brevID",
            onSuccess = { resource ->
                resource.response?.let { deserialize(it.toJson()) }
                    ?: throw InternfeilException("Feil ved generering av pdf vedtaksbrev")
            },
            brukerTokenInfo = brukerTokenInfo,
            postBody = brevRequest,
            timeoutConfig = {
                socketTimeoutMillis = Duration.ofSeconds(30).toMillis()
                requestTimeoutMillis = Duration.ofSeconds(30).toMillis()
            },
        )

    override suspend fun ferdigstillStrukturertBrev(
        behandlingId: UUID,
        brevType: Brevtype,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        post(
            url = "$resourceUrl/api/brev/strukturert/$behandlingId/ferdigstill?brevType=${brevType.name}",
            onSuccess = { _ -> },
            brukerTokenInfo = brukerTokenInfo,
        )
    }

    override suspend fun ferdigstillJournalfoerStrukturertBrev(
        behandlingId: UUID,
        brevType: Brevtype,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        post(
            url = "$resourceUrl/api/brev/strukturert/$behandlingId/ferdigstill-journalfoer-distribuer?brevType=${brevType.name}",
            onSuccess = { _ -> },
            brukerTokenInfo = brukerTokenInfo,
        )
    }

    override suspend fun kanFerdigstilleBrev(
        brevId: BrevID,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): KanFerdigstilleBrevResponse =
        get(
            url = "$resourceUrl/api/brev/$brevId/kan-ferdigstille?${SAKID_CALL_PARAMETER}=${sakId.sakId}",
            onSuccess = { resource ->
                resource.response?.let { deserialize<KanFerdigstilleBrevResponse>(it.toJson()) }
                    ?: throw InternfeilException("Feil oppstod ved sjekk om brev med id $brevId kan ferdigstilles for sak $sakId")
            },
            brukerTokenInfo = brukerTokenInfo,
        )

    override suspend fun tilbakestillStrukturertBrev(
        brevID: BrevID,
        behandlingId: UUID,
        brevRequest: BrevRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevPayload =
        put(
            url = "$resourceUrl/api/brev/strukturert/$behandlingId/tilbakestill?brevId=$brevID",
            onSuccess = { resource ->
                resource.response?.let { deserialize(it.toJson()) }
                    ?: throw InternfeilException("Feil ved tilbakestilling av pdf vedtaksbrev")
            },
            brukerTokenInfo = brukerTokenInfo,
            putBody = brevRequest,
        )

    override suspend fun slettBrev(
        brevSomskalSlettes: BrevID,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        downstreamResourceClient
            .delete(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/brev/$brevSomskalSlettes?${SAKID_CALL_PARAMETER}=${sakId.sakId}",
                    ),
                brukerTokenInfo = brukerTokenInfo,
            ).mapBoth(
                success = { },
                failure = { errorResponse -> throw errorResponse },
            )
    }

    private suspend fun <T> get(
        url: String,
        onSuccess: (Resource) -> T,
        brukerTokenInfo: BrukerTokenInfo,
    ): T =
        downstreamResourceClient
            .get(
                resource = Resource(clientId = clientId, url = url),
                brukerTokenInfo = brukerTokenInfo,
            ).mapBoth(
                success = onSuccess,
                failure = { throwableErrorMessage -> throw throwableErrorMessage },
            )

    private suspend fun <T> post(
        url: String,
        postBody: Any = Unit,
        onSuccess: (Resource) -> T,
        brukerTokenInfo: BrukerTokenInfo,
        timeoutConfig: (HttpTimeoutConfig.() -> Unit)? = null,
    ): T =
        downstreamResourceClient
            .post(
                resource = Resource(clientId = clientId, url = url),
                brukerTokenInfo = brukerTokenInfo,
                postBody = postBody,
                timeoutConfig = timeoutConfig,
            ).mapBoth(
                success = onSuccess,
                failure = { errorResponse -> throw errorResponse },
            )

    private suspend fun <T> put(
        url: String,
        putBody: Any = Unit,
        onSuccess: (Resource) -> T,
        brukerTokenInfo: BrukerTokenInfo,
    ): T =
        downstreamResourceClient
            .put(
                resource = Resource(clientId = clientId, url = url),
                brukerTokenInfo = brukerTokenInfo,
                putBody = putBody,
            ).mapBoth(
                success = onSuccess,
                failure = { errorResponse -> throw errorResponse },
            )
}

// TODO Finnes nå i etterlatte-behandling og brev-api - bør ligge i en lib men er ikke helt landet hvilket lib
// som bør brukes mellom behandling og brev
data class BrevPayload(
    val hoveddel: Slate?,
    val vedlegg: List<BrevInnholdVedlegg>?,
)

data class BrevInnholdVedlegg(
    val tittel: String,
    val key: BrevVedleggKey,
    val payload: Slate? = null,
)
