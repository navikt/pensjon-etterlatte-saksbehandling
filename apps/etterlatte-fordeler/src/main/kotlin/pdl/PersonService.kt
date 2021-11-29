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
    suspend fun hentPerson(fnrListe: List<Foedselsnummer>): Gradering {
        val personer = klient.finnAdressebeskyttelseForFnr(fnrListe).data?.hentPersonBolk?.mapNotNull { it.person }

        if (personer.isNullOrEmpty()) {
            throw Exception("Fant ingen personer i PDL")
        }

        return hentPrioritertGradering(personer)
            .also { logger.info("Gradering vurdert til $it") }
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
