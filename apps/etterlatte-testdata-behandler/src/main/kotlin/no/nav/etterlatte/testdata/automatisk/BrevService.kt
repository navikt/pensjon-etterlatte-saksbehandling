package no.nav.etterlatte.testdata.automatisk

import com.github.michaelbull.result.mapBoth
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.Systembruker
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
    ) {
        val brev = opprettVedtaksbrev(behandlingId, sakId)
        genererPDF(behandlingId, brev)
        ferdigstillBrev(behandlingId)
    }

    private suspend fun opprettVedtaksbrev(
        behandlingId: UUID,
        sakId: Long,
    ): Brev =
        klient
            .post(Resource(clientId, "$url/api/brev/behandling/$behandlingId/vedtak?sakId=$sakId"), Systembruker.testdata) {}
            .mapBoth(
                success = { readValue(it) },
                failure = { throw it },
            )

    private suspend fun genererPDF(
        behandlingId: UUID,
        brev: Brev,
    ) = klient.get(Resource(clientId, "$url/api/brev/behandling/$behandlingId/vedtak/pdf?brevId=${brev.id}"), Systembruker.testdata)

    private suspend fun ferdigstillBrev(behandlingId: UUID) =
        klient.post(Resource(clientId, "$url/api/brev/behandling/$behandlingId/vedtak/ferdigstill"), Systembruker.testdata) {
        }
}
