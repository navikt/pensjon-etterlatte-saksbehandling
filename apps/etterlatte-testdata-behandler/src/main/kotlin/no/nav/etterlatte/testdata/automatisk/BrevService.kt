package no.nav.etterlatte.testdata.automatisk

import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.readValue
import org.slf4j.LoggerFactory
import java.util.UUID

class BrevService(
    private val klient: DownstreamResourceClient,
    private val url: String,
    private val clientId: String,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun opprettOgDistribuerVedtaksbrev(
        sakId: SakId,
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ) {
        val brev = opprettVedtaksbrev(behandlingId, sakId, bruker)
        logger.info("Genererte brev $brev i sak $sakId")
        genererPDF(behandlingId, brev, bruker)
        ferdigstillBrev(behandlingId, bruker)
    }

    private suspend fun opprettVedtaksbrev(
        behandlingId: UUID,
        sakId: SakId,
        bruker: BrukerTokenInfo,
    ): Brev =
        klient
            .post(Resource(clientId, "$url/api/behandling/brev/$behandlingId?sakId=$sakId"), bruker, {})
            .mapBoth(
                success = { readValue(it) },
                failure = { throw it },
            )

    private suspend fun genererPDF(
        behandlingId: UUID,
        brev: Brev,
        bruker: BrukerTokenInfo,
    ) = klient
        .get(Resource(clientId, "$url/api/behandling/brev/$behandlingId/pdf?brevId=${brev.id}&sakId=${brev.sakId}"), bruker)
        .mapError { throw it }

    private suspend fun ferdigstillBrev(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ) = klient
        .post(Resource(clientId, "$url/api/behandling/brev/$behandlingId/ferdigstill"), bruker, {})
        .mapError { throw it }
}
