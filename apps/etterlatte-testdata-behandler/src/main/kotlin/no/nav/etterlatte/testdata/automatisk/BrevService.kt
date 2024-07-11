package no.nav.etterlatte.testdata.automatisk

import com.github.michaelbull.result.mapBoth
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.readValue
import java.util.UUID

class BrevService(
    private val klient: DownstreamResourceClient,
    private val url: String,
    private val clientId: String,
) {
    suspend fun opprettOgDistribuerVedtaksbrev(
        sakId: Long,
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ) {
        val brev = opprettVedtaksbrev(behandlingId, sakId, bruker)
        genererPDF(behandlingId, brev, bruker)
        ferdigstillBrev(behandlingId, bruker)
    }

    private suspend fun opprettVedtaksbrev(
        behandlingId: UUID,
        sakId: Long,
        bruker: BrukerTokenInfo,
    ): Brev =
        klient
            .post(Resource(clientId, "$url/api/brev/behandling/$behandlingId/vedtak?sakId=$sakId"), bruker) {}
            .mapBoth(
                success = { readValue(it) },
                failure = { throw it },
            )

    private suspend fun genererPDF(
        behandlingId: UUID,
        brev: Brev,
        bruker: BrukerTokenInfo,
    ) = klient.get(Resource(clientId, "$url/api/brev/behandling/$behandlingId/vedtak/pdf?brevId=${brev.id}"), bruker)

    private suspend fun ferdigstillBrev(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ) = klient.post(Resource(clientId, "$url/api/brev/behandling/$behandlingId/vedtak/ferdigstill"), bruker) {
    }
}
