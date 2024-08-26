package no.nav.etterlatte.libs.ktor.route

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import java.util.UUID

interface BehandlingTilgangsSjekk {
    suspend fun harTilgangTilBehandling(
        behandlingId: UUID,
        skrivetilgang: Boolean = false,
        bruker: Saksbehandler,
    ): Boolean
}

interface SakTilgangsSjekk {
    suspend fun harTilgangTilSak(
        sakId: SakId,
        skrivetilgang: Boolean = false,
        bruker: Saksbehandler,
    ): Boolean
}

interface PersonTilgangsSjekk {
    suspend fun harTilgangTilPerson(
        foedselsnummer: Folkeregisteridentifikator,
        skrivetilgang: Boolean = false,
        bruker: Saksbehandler,
    ): Boolean
}
