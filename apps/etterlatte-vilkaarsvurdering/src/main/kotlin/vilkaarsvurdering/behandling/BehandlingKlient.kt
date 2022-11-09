package no.nav.etterlatte.vilkaarsvurdering.behandling

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.retry
import org.slf4j.LoggerFactory
import java.util.UUID

interface BehandlingKlient {
    suspend fun hentBehandling(behandlingId: UUID): RetryResult<DetaljertBehandling>
}

class BehandlingKlientImpl(private val behandlingUrl: String, private val httpClient: HttpClient) : BehandlingKlient {
    private val logger = LoggerFactory.getLogger(BehandlingKlient::class.java)

    override suspend fun hentBehandling(behandlingId: UUID): RetryResult<DetaljertBehandling> {
        logger.info("Henter behandling med id $behandlingId")

        return retry {
            httpClient.get("$behandlingUrl/behandlinger/$behandlingId").body()
        }
    }
}