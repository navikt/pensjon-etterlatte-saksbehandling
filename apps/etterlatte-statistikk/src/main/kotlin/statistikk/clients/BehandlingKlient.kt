package no.nav.etterlatte.statistikk.clients

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetspliktDto
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.StatistikkBehandling
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.sak.SakId
import java.util.UUID

interface BehandlingKlient {
    suspend fun hentPersongalleri(behandlingId: UUID): Persongalleri

    suspend fun hentStatistikkBehandling(behandlingId: UUID): StatistikkBehandling

    suspend fun hentUtlandstilknytning(behandlingId: UUID): Utlandstilknytning?

    suspend fun hentAktivitetspliktDto(
        sakId: SakId,
        behandlingId: UUID,
    ): AktivitetspliktDto
}

class BehandlingKlientImpl(
    private val behandlingHttpClient: HttpClient,
    private val behandlingUrl: String,
) : BehandlingKlient {
    override suspend fun hentPersongalleri(behandlingId: UUID): Persongalleri = hentStatistikkBehandling(behandlingId).toPersongalleri()

    override suspend fun hentStatistikkBehandling(behandlingId: UUID): StatistikkBehandling =
        try {
            behandlingHttpClient
                .get("$behandlingUrl/behandlinger/statistikk/$behandlingId")
                .body()
        } catch (e: Exception) {
            throw KunneIkkeHenteFraBehandling("Kunne ikke hente behandling med id $behandlingId fra Behandling", e)
        }

    override suspend fun hentUtlandstilknytning(behandlingId: UUID): Utlandstilknytning? =
        hentStatistikkBehandling(behandlingId).utlandstilknytning

    override suspend fun hentAktivitetspliktDto(
        sakId: SakId,
        behandlingId: UUID,
    ): AktivitetspliktDto =
        try {
            behandlingHttpClient
                .get("$behandlingUrl/api/sak/$sakId/aktivitetsplikt/statistikk/$behandlingId")
                .body()
        } catch (e: Exception) {
            throw KunneIkkeHenteFraBehandling("Kunne ikke hente aktivitetspliktDto for sak $sakId fra Behandling", e)
        }
}

class KunneIkkeHenteFraBehandling(
    message: String,
    cause: Exception,
) : Exception(message, cause)

fun StatistikkBehandling.toPersongalleri() =
    Persongalleri(
        soeker = this.soeker,
        innsender = this.innsender,
        soesken = this.soesken ?: emptyList(),
        avdoed = this.avdoed ?: emptyList(),
        gjenlevende = this.gjenlevende ?: emptyList(),
    )
