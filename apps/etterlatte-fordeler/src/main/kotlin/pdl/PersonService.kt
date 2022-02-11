package no.nav.etterlatte.pdl

import no.nav.etterlatte.libs.common.pdl.AdressebeskyttelsePerson
import no.nav.etterlatte.libs.common.pdl.EyHentAdresseRequest
import no.nav.etterlatte.libs.common.pdl.Gradering
import no.nav.etterlatte.libs.common.person.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*


class PersonService(private val klient: Pdl) {
    private val logger = LoggerFactory.getLogger(PersonService::class.java)

    /**
     * Henter ut adressebeskyttelse-gradering basert liste over identer/fnr.
     *
     * @param fnrListe: Liste over fødselsnummere
     *
     * @return Gyldig gradering av PDL-typen [Gradering].
     *  Gir verdi [Gradering.UGRADERT] dersom ingenting er funnet.
     */
    suspend fun hentPerson(fnr: Foedselsnummer): Person {
        //TODO exception håndteringen virker ikke
        val person = klient.hentPerson(fnr) ?: throw Exception("Fant ingen personer i PDL")

        return person
            .also { logger.info("Person funnet") }
    }
    suspend fun hentUtland(fnr: Foedselsnummer): eyUtland {
        //TODO exception håndteringen virker ikke
        val person = klient.hentUtland(fnr) ?: throw Exception("Fant ingen personer i PDL")

        return person
            .also { logger.info("Hentet utlandsinfo for Person") }
    }
    suspend fun hentAdresse(fnr: Foedselsnummer, historikk: Boolean): eyAdresse {
        //TODO exception håndteringen virker ikke
        val person = klient.hentAdresse(fnr, historikk = historikk) ?: throw Exception("Fant ingen personer i PDL")

        return person
            .also { logger.info("Hentet adresse for Person") }
    }
    suspend fun hentFamilieForhold(fnr: Foedselsnummer): EyFamilieRelasjon {
        //TODO exception håndteringen virker ikke
        val person = klient.hentFamilieRelasjon(fnr) ?: throw Exception("Fant ingen personer i PDL")

        return person
            .also { logger.info("Hentet familierelasjon for Person") }
    }
    suspend fun hentUtvidetPerson(fnr: Foedselsnummer): Person {
        //TODO exception håndteringen virker ikke
        val person = klient.hentUtvidetPerson(fnr) ?: throw Exception("Fant ingen personer i PDL")

        return person
            .also { logger.info("Person funnet") }
    }

    /**
     * Henter ut alle graderinger fra liste over personer og deretter henter prioritert [Gradering]
     *
     * @return [Gradering]
     */
    private fun hentPrioritertGradering(personer: List<AdressebeskyttelsePerson>): Gradering =
        personer.flatMap { it.adressebeskyttelse }
            .mapNotNull { it.gradering }
            .minOrNull() ?: Gradering.UGRADERT
}
