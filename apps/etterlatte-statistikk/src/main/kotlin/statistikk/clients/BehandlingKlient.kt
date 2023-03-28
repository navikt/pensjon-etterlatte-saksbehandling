package no.nav.etterlatte.statistikk.clients

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import java.util.UUID

interface BehandlingKlient {
    suspend fun hentPersongalleri(behandlingId: UUID): Persongalleri
    suspend fun hentDetaljertBehandling(behandlingId: UUID): DetaljertBehandling
}

class BehandlingKlientImpl(
    private val behandlingHttpClient: HttpClient,
    private val behandlingUrl: String
) : BehandlingKlient {

    override suspend fun hentPersongalleri(behandlingId: UUID): Persongalleri {
        return hentDetaljertBehandling(behandlingId).toPersongalleri()
    }

    override suspend fun hentDetaljertBehandling(behandlingId: UUID): DetaljertBehandling {
        return try {
            behandlingHttpClient.get("$behandlingUrl/behandlinger/$behandlingId")
                .body()
        } catch (e: Exception) {
            throw KunneIkkeHenteFraBehandling("Kunne ikke hente behandling med id $behandlingId fra Behandling", e)
        }
    }
}

class KunneIkkeHenteFraBehandling(message: String, cause: Exception) : Exception(message, cause)

fun DetaljertBehandling.toPersongalleri() = Persongalleri(
    soeker = this.soeker,
    innsender = this.innsender,
    soesken = this.soesken ?: emptyList(),
    avdoed = this.avdoed ?: emptyList(),
    gjenlevende = this.gjenlevende ?: emptyList()
)