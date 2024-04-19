package no.nav.etterlatte.no.nav.etterlatte.testdata.automatisk

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import no.nav.etterlatte.brev.model.Brev
import java.util.UUID

class BrevService(private val klient: HttpClient, private val url: String) {
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
    ) = klient.post("$url/brev/behandling/$behandlingId/vedtak?sakId=$sakId").body<Brev>()

    private suspend fun genererPDF(
        behandlingId: UUID,
        brev: Brev,
    ) = klient.post("$url/brev/behandling/$behandlingId/vedtak/pdf?brevId=${brev.id}")

    private suspend fun ferdigstillBrev(behandlingId: UUID) = klient.post("$url/brev/behandling/$behandlingId/vedtak/ferdigstill")
}
