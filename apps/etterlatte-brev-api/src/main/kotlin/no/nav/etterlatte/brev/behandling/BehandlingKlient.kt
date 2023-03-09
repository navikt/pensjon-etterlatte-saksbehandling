package no.nav.etterlatte.brev.tilgangssjekk

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.BehandlingTilgangsSjekk
import no.nav.etterlatte.token.Saksbehandler
import java.util.*

class BehandlingKlient(config: Config, httpClient: HttpClient) : BehandlingTilgangsSjekk {
    override fun harTilgangTilBehandling(behandlingId: UUID, bruker: Saksbehandler): Boolean {
        return true
    }
}