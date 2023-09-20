package no.nav.etterlatte.behandling

import no.nav.etterlatte.behandling.domain.SaksbehandlerEnhet
import no.nav.etterlatte.behandling.klienter.NavAnsattKlient

interface EnhetService {
    suspend fun enheterForIdent(ident: String): List<SaksbehandlerEnhet>

    suspend fun harTilgangTilEnhet(
        ident: String,
        enhetId: String,
    ): Boolean
}

class EnhetServiceImpl(val client: NavAnsattKlient) : EnhetService {
    override suspend fun enheterForIdent(ident: String) = client.hentSaksbehandlerEnhet(ident)

    override suspend fun harTilgangTilEnhet(
        ident: String,
        enhetId: String,
    ) = enheterForIdent(ident).any { enhet -> enhet.id == enhetId }
}
