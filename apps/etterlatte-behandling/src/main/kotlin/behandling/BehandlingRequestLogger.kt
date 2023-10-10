package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.sporingslogg.Decision
import no.nav.etterlatte.libs.sporingslogg.HttpMethod
import no.nav.etterlatte.libs.sporingslogg.Sporingslogg
import no.nav.etterlatte.libs.sporingslogg.Sporingsrequest
import no.nav.etterlatte.token.BrukerTokenInfo

class BehandlingRequestLogger(private val sporingslogg: Sporingslogg) {
    fun loggRequest(
        brukerTokenInfo: BrukerTokenInfo,
        fnr: Folkeregisteridentifikator,
    ) = sporingslogg.logg(
        Sporingsrequest(
            kallendeApplikasjon = "behandling",
            oppdateringstype = HttpMethod.GET,
            brukerId = brukerTokenInfo.ident(),
            hvemBlirSlaattOpp = fnr.value,
            endepunkt = "behandling",
            resultat = Decision.Permit,
            melding = "Hent behandling var vellykka",
        ),
    )
}
