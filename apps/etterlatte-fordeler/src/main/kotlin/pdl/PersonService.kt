package no.nav.etterlatte.pdl

import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.Person
import org.slf4j.LoggerFactory

class PersonService(private val klient: Pdl) {

    private val logger = LoggerFactory.getLogger(PersonService::class.java)

    suspend fun hentPerson(
        fnr: Foedselsnummer,
        historikk: Boolean = false,
        adresse: Boolean = false,
        utland: Boolean = false,
        familieRelasjon: Boolean = false
    ): Person {
        //TODO exception håndteringen virker ikke
        logger.info("Henter $fnr fra pdltjenester")
        return klient.hentPerson(HentPersonRequest(
            foedselsnummer = fnr,
            historikk = historikk,
            adresse = adresse,
            utland = utland,
            familieRelasjon = familieRelasjon
        )) ?: throw Exception("Fant ingen personer i PDL")
    }

}
