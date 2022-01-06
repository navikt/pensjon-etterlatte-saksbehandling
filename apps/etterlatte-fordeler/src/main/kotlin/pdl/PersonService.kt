package no.nav.etterlatte.pdl

import no.nav.etterlatte.libs.common.pdl.AdressebeskyttelsePerson
import no.nav.etterlatte.libs.common.pdl.Gradering
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import org.slf4j.LoggerFactory

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
    //TODO Fjerne?
    //suspend fun hentAlderForPerson(fnr: Foedselsnummer): Int {
     //   val foedselsAar = klient.hentPerson(fnr).data?.hentPerson?.foedsel?.mapNotNull  { it.foedselsaar }
        //TODO
        //definere år på en smart måte og støtte for å sjekke liste evt
        //også må vi håndtere null
     //   return 2021 - foedselsAar?.get(0)!!
    //}

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
