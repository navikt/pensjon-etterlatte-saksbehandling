package no.nav.etterlatte.person

import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.FolkeregisterIdent
import no.nav.etterlatte.libs.common.person.HentFolkeregisterIdentRequest
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.ktor.Saksbehandler
import no.nav.etterlatte.libs.sporingslogg.Decision
import no.nav.etterlatte.libs.sporingslogg.HttpMethod
import no.nav.etterlatte.libs.sporingslogg.Sporingslogg
import no.nav.etterlatte.libs.sporingslogg.Sporingsrequest
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlKlient
import no.nav.etterlatte.pdl.PdlResponseError
import no.nav.etterlatte.pdl.mapper.PersonMapper
import org.slf4j.LoggerFactory

class PdlForesporselFeilet(message: String) : RuntimeException(message)
class PdlFantIkkePerson(message: String) : RuntimeException(message)

class PersonService(
    private val pdlKlient: PdlKlient,
    private val ppsKlient: ParallelleSannheterKlient,
    private val sporingslogg: Sporingslogg
) {
    private val logger = LoggerFactory.getLogger(PersonService::class.java)

    suspend fun hentPerson(hentPersonRequest: HentPersonRequest, saksbehandler: Saksbehandler): Person {
        logger.info("Henter person med fnr=${hentPersonRequest.foedselsnummer} fra PDL")

        return pdlKlient.hentPerson(hentPersonRequest.foedselsnummer, hentPersonRequest.rolle).let {
            if (it.data?.hentPerson == null) {
                sporingslogg.logg(
                    feilendeRequest(
                        ident = hentPersonRequest.foedselsnummer.value,
                        bruker = saksbehandler,
                        endepunkt = "person"
                    )
                )
                val pdlFeil = it.errors?.asFormatertFeil()
                if (it.errors?.personIkkeFunnet() == true) {
                    throw PdlFantIkkePerson("Fant ikke personen ${hentPersonRequest.foedselsnummer}")
                } else {
                    throw PdlForesporselFeilet(
                        "Kunne ikke hente person med fnr=${hentPersonRequest.foedselsnummer} fra PDL: $pdlFeil"
                    )
                }
            } else {
                sporingslogg.logg(
                    vellykkaRequest(
                        ident = hentPersonRequest.foedselsnummer.value,
                        bruker = saksbehandler,
                        endepunkt = "person"
                    )
                )
                PersonMapper.mapPerson(
                    ppsKlient = ppsKlient,
                    pdlKlient = pdlKlient,
                    fnr = hentPersonRequest.foedselsnummer,
                    personRolle = hentPersonRequest.rolle,
                    hentPerson = it.data.hentPerson
                )
            }
        }
    }

    private fun vellykkaRequest(ident: String, bruker: Saksbehandler, endepunkt: String) = Sporingsrequest(
        kallendeApplikasjon = "pdltjenester",
        oppdateringstype = HttpMethod.GET,
        brukerId = bruker.ident,
        hvemBlirSlaattOpp = ident,
        endepunkt = "/$endepunkt",
        resultat = Decision.Permit,
        melding = "Hent $endepunkt var vellykka"
    )

    private fun feilendeRequest(ident: String, bruker: Saksbehandler, endepunkt: String) = Sporingsrequest(
        kallendeApplikasjon = "api",
        oppdateringstype = HttpMethod.GET,
        brukerId = bruker.ident,
        hvemBlirSlaattOpp = ident,
        endepunkt = "/$endepunkt",
        resultat = Decision.Deny,
        melding = "Hent $endepunkt feilet"
    )

    suspend fun hentOpplysningsperson(hentPersonRequest: HentPersonRequest, saksbehandler: Saksbehandler): PersonDTO {
        logger.info("Henter opplysninger for person med fnr=${hentPersonRequest.foedselsnummer} fra PDL")

        return pdlKlient.hentPerson(hentPersonRequest.foedselsnummer, hentPersonRequest.rolle).let {
            if (it.data?.hentPerson == null) {
                sporingslogg.logg(
                    feilendeRequest(
                        ident = hentPersonRequest.foedselsnummer.value,
                        bruker = saksbehandler,
                        endepunkt = "opplysningsperson"
                    )
                )
                val pdlFeil = it.errors?.asFormatertFeil()
                if (it.errors?.personIkkeFunnet() == true) {
                    throw PdlFantIkkePerson("Fant ikke personen ${hentPersonRequest.foedselsnummer}")
                } else {
                    throw PdlForesporselFeilet(
                        "Kunne ikke hente opplysninger for ${hentPersonRequest.foedselsnummer} fra PDL: $pdlFeil"
                    )
                }
            } else {
                sporingslogg.logg(
                    vellykkaRequest(
                        ident = hentPersonRequest.foedselsnummer.value,
                        bruker = saksbehandler,
                        endepunkt = "opplysningsperson"
                    )
                )
                PersonMapper.mapOpplysningsperson(
                    ppsKlient = ppsKlient,
                    pdlKlient = pdlKlient,
                    fnr = hentPersonRequest.foedselsnummer,
                    personRolle = hentPersonRequest.rolle,
                    hentPerson = it.data.hentPerson
                )
            }
        }
    }

    suspend fun hentFolkeregisterIdent(
        hentFolkeregisterIdentRequest: HentFolkeregisterIdentRequest,
        saksbehandler: Saksbehandler
    ): FolkeregisterIdent {
        logger.info("Henter folkeregisterident for ident=${hentFolkeregisterIdentRequest.ident} fra PDL")

        return pdlKlient.hentFolkeregisterIdent(hentFolkeregisterIdentRequest.ident).let {
            if (it.data?.hentIdenter == null) {
                sporingslogg.logg(
                    feilendeRequest(
                        ident = hentFolkeregisterIdentRequest.ident,
                        bruker = saksbehandler,
                        endepunkt = "folkeregisterIdent"
                    )
                )
                val pdlFeil = it.errors?.asFormatertFeil()
                if (it.errors?.personIkkeFunnet() == true) {
                    throw PdlFantIkkePerson("Fant ikke personen ${hentFolkeregisterIdentRequest.ident}")
                } else {
                    throw PdlForesporselFeilet(
                        "Kunne ikke hente folkeregisterident " +
                            "for ${hentFolkeregisterIdentRequest.ident} fra PDL: $pdlFeil"
                    )
                }
            } else {
                try {
                    val folkeregisterIdent: String = it.data.hentIdenter.identer
                        .filter { it.gruppe == "FOLKEREGISTERIDENT" }
                        .first { !it.historisk }.ident
                    FolkeregisterIdent(folkeregisterident = Foedselsnummer.of(folkeregisterIdent))
                } catch (e: Exception) {
                    throw PdlForesporselFeilet(
                        "Fant ingen folkeregisterident for ${hentFolkeregisterIdentRequest.ident} fra PDL"
                    )
                }
            }
        }
    }

    fun List<PdlResponseError>.asFormatertFeil() = this.joinToString(", ")
    fun List<PdlResponseError>.personIkkeFunnet() = any { it.extensions?.code == "not_found" }
}