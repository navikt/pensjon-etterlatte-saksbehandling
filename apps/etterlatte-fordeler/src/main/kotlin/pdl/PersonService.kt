package no.nav.etterlatte.pdl

import no.nav.etterlatte.libs.common.person.Foedselsnummer
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
        val person = klient.hentPerson(fnr,historikk,adresse,utland,familieRelasjon) ?: throw Exception("Fant ingen personer i PDL")

        return person
            .also { logger.info("Person funnet") }
    }

}
