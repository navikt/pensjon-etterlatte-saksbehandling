package no.nav.etterlatte.person

import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle.*
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlKlient
import no.nav.etterlatte.pdl.PdlVariables
import no.nav.etterlatte.pdl.mapper.PersonMapper
import org.slf4j.LoggerFactory

class PdlForesporselFeilet(message: String) : RuntimeException(message)

class PersonService(
    private val pdlKlient: PdlKlient,
    private val ppsKlient: ParallelleSannheterKlient
) {
    private val logger = LoggerFactory.getLogger(PersonService::class.java)

    suspend fun hentPerson(hentPersonRequest: HentPersonRequest): Person {
        logger.info("Henter person med fnr=${hentPersonRequest.foedselsnummer} fra PDL")

        return pdlKlient.hentPerson(hentPersonRequest.toPdlVariables()).let {
            if (it.data?.hentPerson == null) {
                val pdlFeil = it.errors?.joinToString(", ")
                throw PdlForesporselFeilet(
                    "Kunne ikke hente person med fnr=${hentPersonRequest.foedselsnummer} fra PDL: $pdlFeil"
                )
            } else {
                PersonMapper.mapPerson(
                    ppsKlient = ppsKlient,
                    fnr = hentPersonRequest.foedselsnummer,
                    personRolle = hentPersonRequest.rolle,
                    hentPerson = it.data.hentPerson
                )
            }
        }
    }

    private fun HentPersonRequest.toPdlVariables() =
        when (rolle) {
            BARN ->
                PdlVariables(
                    ident = foedselsnummer.value,
                    bostedsadresse = true,
                    bostedsadresseHistorikk = true,
                    deltBostedsadresse = true,
                    kontaktadresse = true,
                    kontaktadresseHistorikk = true,
                    oppholdsadresse = true,
                    oppholdsadresseHistorikk = true,
                    utland = true,
                    sivilstand = false,
                    familieRelasjon = true
                )
            GJENLEVENDE ->
                PdlVariables(
                    ident = foedselsnummer.value,
                    bostedsadresse = true,
                    bostedsadresseHistorikk = true,
                    deltBostedsadresse = false,
                    kontaktadresse = false,
                    kontaktadresseHistorikk = false,
                    oppholdsadresse = true,
                    oppholdsadresseHistorikk = false,
                    utland = true,
                    sivilstand = true,
                    familieRelasjon = true
                )
            AVDOED ->
                PdlVariables(
                    ident = foedselsnummer.value,
                    bostedsadresse = true,
                    bostedsadresseHistorikk = true,
                    deltBostedsadresse = false,
                    kontaktadresse = true,
                    kontaktadresseHistorikk = true,
                    oppholdsadresse = true,
                    oppholdsadresseHistorikk = true,
                    utland = true,
                    sivilstand = true,
                    familieRelasjon = true
                )
    }

}
