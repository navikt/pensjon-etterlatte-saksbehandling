package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.sporingslogg.Decision
import no.nav.etterlatte.libs.sporingslogg.HttpMethod
import no.nav.etterlatte.libs.sporingslogg.Sporingslogg
import no.nav.etterlatte.libs.sporingslogg.Sporingsrequest

class BehandlingRequestLogger(
    private val sporingslogg: Sporingslogg,
) {
    fun loggRequest(
        brukerTokenInfo: BrukerTokenInfo,
        fnr: Folkeregisteridentifikator,
        endepunkt: String,
    ) = sporingslogg.logg(
        Sporingsrequest(
            kallendeApplikasjon = "behandling",
            oppdateringstype = HttpMethod.GET,
            brukerId = brukerTokenInfo.ident(),
            hvemBlirSlaattOpp = fnr.value,
            endepunkt = endepunkt,
            resultat = Decision.Permit,
            melding = "Hent behandling var vellykka",
        ),
    )
}
