package no.nav.etterlatte.behandling

import no.nav.etterlatte.behandling.domain.SaksbehandlerEnhet
import no.nav.etterlatte.behandling.klienter.NavAnsattKlient

interface EnhetService {
    fun enheterForIdent(ident: String): List<SaksbehandlerEnhet>

    fun harTilgangTilEnhet(ident: String, enhetId: String): Boolean
}

class EnhetServiceImpl(val client: NavAnsattKlient) : EnhetService {
    override fun enheterForIdent(ident: String) = client.hentSaksbehandlerEnhet(ident)

    override fun harTilgangTilEnhet(ident: String, enhetId: String) =
        enheterForIdent(ident).any { enhet -> enhet.id == enhetId }
}