package no.nav.etterlatte.brev

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

class Pdf(
    val bytes: ByteArray,
)

class BrevKlient(
    config: Config,
    client: HttpClient,
) {
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, client)

    private val clientId = config.getString("brev-api.client.id")
    private val resourceUrl = config.getString("brev-api.resource.url")

    suspend fun opprettVedtaksbrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        brevRequest: BrevRequest,
    ): Brev =
        post(
            url = "$resourceUrl/api/brev/tilbakekreving/$behandlingId/vedtak",
            onSuccess = { resource -> deserialize(resource.response!!.toJson()) },
            brukerTokenInfo = brukerTokenInfo,
            postBody = brevRequest,
        )

    suspend fun genererPdf(
        brevID: BrevID,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        brevRequest: BrevRequest,
    ): Pdf =
        post(
            url = "$resourceUrl/api/brev/tilbakekreving/$behandlingId/vedtak/pdf?brevId=$brevID",
            onSuccess = { resource ->
                resource.response?.let { deserialize(it.toJson()) }
                    ?: throw InternfeilException("Feil ved generering av pdf vedtaksbrev")
            },
            brukerTokenInfo = brukerTokenInfo,
            postBody = brevRequest,
        )

    suspend fun ferdigstillVedtaksbrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        post(
            url = "$resourceUrl/api/brev/tilbakekreving/$behandlingId/vedtak/ferdigstill",
            onSuccess = { _ -> },
            brukerTokenInfo = brukerTokenInfo,
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
    ): T =
        downstreamResourceClient
            .post(
                resource = Resource(clientId = clientId, url = url),
                brukerTokenInfo = brukerTokenInfo,
                postBody = postBody,
            ).mapBoth(
                success = onSuccess,
                failure = { errorResponse -> throw errorResponse },
            )
}
