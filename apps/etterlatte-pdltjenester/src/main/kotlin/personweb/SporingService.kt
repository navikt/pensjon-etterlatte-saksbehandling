package no.nav.etterlatte.personweb

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.sporingslogg.Decision
import no.nav.etterlatte.libs.sporingslogg.HttpMethod
import no.nav.etterlatte.libs.sporingslogg.Sporingslogg
import no.nav.etterlatte.libs.sporingslogg.Sporingsrequest
import no.nav.etterlatte.token.BrukerTokenInfo

class SporingService(private val sporingslogg: Sporingslogg) {
    fun logg(
        brukerTokenInfo: BrukerTokenInfo,
        fnr: Folkeregisteridentifikator,
        endepunkt: String,
        melding: String,
    ) = sporingslogg.logg(
        Sporingsrequest(
            kallendeApplikasjon = "etterlatte-pdltjenester",
            oppdateringstype = HttpMethod.GET,
            brukerId = brukerTokenInfo.ident(),
            hvemBlirSlaattOpp = fnr.value,
            endepunkt = endepunkt,
            resultat = Decision.Permit,
            melding = melding,
        ),
    )
}
