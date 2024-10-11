package no.nav.etterlatte.utbetaling.klienter

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.ktor.route.BehandlingTilgangsSjekk
import no.nav.etterlatte.libs.ktor.route.Tilgangssjekker
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import java.util.UUID

class BehandlingKlient(
    config: Config,
    httpClient: HttpClient,
) : BehandlingTilgangsSjekk {
    private val tilgangssjekker = Tilgangssjekker(config, httpClient)

    override suspend fun harTilgangTilBehandling(
        behandlingId: UUID,
        skrivetilgang: Boolean,
        bruker: Saksbehandler,
    ): Boolean = tilgangssjekker.harTilgangTilBehandling(behandlingId, skrivetilgang, bruker)
}
