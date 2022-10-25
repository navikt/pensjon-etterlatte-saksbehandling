package no.nav.etterlatte.statistikk.clients

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import java.util.UUID

interface BehandlingClient {
    suspend fun hentPersongalleri(behandlingId: UUID): Persongalleri
}

class BehandlingClientImpl(private val behandlingHttpClient: HttpClient) : BehandlingClient {

    override suspend fun hentPersongalleri(behandlingId: UUID): Persongalleri {
        return behandlingHttpClient.get("behandlinger/$behandlingId")
            .body<DetaljertBehandling>().toPersongalleri()
    }
}

fun DetaljertBehandling.toPersongalleri() = Persongalleri(
    soeker = this.soeker!!,
    innsender = this.innsender,
    soesken = this.soesken ?: emptyList(),
    avdoed = this.avdoed ?: emptyList(),
    gjenlevende = this.gjenlevende ?: emptyList()
)