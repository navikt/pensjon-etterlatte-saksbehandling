package no.nav.etterlatte.statistikk.clients

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.sak.Sak
import java.util.UUID

interface BehandlingClient {
    suspend fun hentPersongalleri(behandlingId: UUID): Persongalleri
    suspend fun hentDetaljertBehandling(behandlingId: UUID): DetaljertBehandling
    suspend fun hentSak(sakId: Long): Sak
}

class BehandlingClientImpl(private val behandlingHttpClient: HttpClient) : BehandlingClient {

    override suspend fun hentPersongalleri(behandlingId: UUID): Persongalleri {
        return hentDetaljertBehandling(behandlingId).toPersongalleri()
    }

    override suspend fun hentDetaljertBehandling(behandlingId: UUID): DetaljertBehandling {
        return try {
            behandlingHttpClient.get("behandlinger/$behandlingId")
                .body()
        } catch (e: Exception) {
            throw KunneIkkeHenteFraBehandling("Kunne ikke hente behandling med id $behandlingId fra Behandling", e)
        }
    }

    override suspend fun hentSak(sakId: Long): Sak {
        return try {
            behandlingHttpClient.get("saker/$sakId")
                .body()
        } catch (e: Exception) {
            throw KunneIkkeHenteFraBehandling("Kunne ikke hente sak med id $sakId fra Behandling", e)
        }
    }
}

class KunneIkkeHenteFraBehandling(message: String, cause: Exception) : Exception(message, cause)

fun DetaljertBehandling.toPersongalleri() = Persongalleri(
    soeker = this.soeker!!,
    innsender = this.innsender,
    soesken = this.soesken ?: emptyList(),
    avdoed = this.avdoed ?: emptyList(),
    gjenlevende = this.gjenlevende ?: emptyList()
)