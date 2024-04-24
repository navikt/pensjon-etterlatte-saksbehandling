package no.nav.etterlatte.testdata.automatisk

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.Systembruker
import java.util.UUID

class BrevService(private val klient: DownstreamResourceClient, private val url: String, private val clientId: String) {
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
        klient.post(Resource(clientId, "$url/brev/behandling/$behandlingId/vedtak?sakId=$sakId"), Systembruker.testdata) {}
            .mapBoth(
                success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                failure = { throw it },
            )

    private suspend fun genererPDF(
        behandlingId: UUID,
        brev: Brev,
    ) = klient.post(Resource(clientId, "$url/brev/behandling/$behandlingId/vedtak/pdf?brevId=${brev.id}"), Systembruker.testdata) {}

    private suspend fun ferdigstillBrev(behandlingId: UUID) =
        klient.post(Resource(clientId, "$url/brev/behandling/$behandlingId/vedtak/ferdigstill"), Systembruker.testdata) {
        }
}
