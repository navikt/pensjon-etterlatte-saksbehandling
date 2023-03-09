package no.nav.etterlatte.brev.tilgangssjekk

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.TilgangsSjekk
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.token.Saksbehandler
import java.util.*

class BehandlingKlient(config: Config, httpClient: HttpClient) : TilgangsSjekk {
    override fun harTilgangTilBehandling(behandlingId: UUID, bruker: Saksbehandler): Boolean {
        return true
    }

    override fun harTilgangTilSak(sakId: Long, bruker: Saksbehandler): Boolean {
        TODO("Not yet implemented")
    }

    override fun harTilgangTilPerson(behandlingId: Foedselsnummer, bruker: Saksbehandler): Boolean {
        TODO("Not yet implemented")
    }
}