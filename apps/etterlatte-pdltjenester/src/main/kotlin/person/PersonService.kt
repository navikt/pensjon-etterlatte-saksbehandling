package no.nav.etterlatte.person

import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.FolkeregisterIdent
import no.nav.etterlatte.libs.common.person.HentFolkeregisterIdentRequest
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlKlient
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

        return pdlKlient.hentPerson(hentPersonRequest.foedselsnummer, hentPersonRequest.rolle).let {
            if (it.data?.hentPerson == null) {
                val pdlFeil = it.errors?.joinToString(", ")
                throw PdlForesporselFeilet(
                    "Kunne ikke hente person med fnr=${hentPersonRequest.foedselsnummer} fra PDL: $pdlFeil"
                )
            } else {
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

    suspend fun hentOpplysningsperson(hentPersonRequest: HentPersonRequest): PersonDTO {
        logger.info("Henter opplysninger for person med fnr=${hentPersonRequest.foedselsnummer} fra PDL")

        return pdlKlient.hentPerson(hentPersonRequest.foedselsnummer, hentPersonRequest.rolle).let {
            if (it.data?.hentPerson == null) {
                val pdlFeil = it.errors?.joinToString(", ")
                throw PdlForesporselFeilet(
                    "Kunne ikke hente opplysninger for person med fnr=" +
                        "${hentPersonRequest.foedselsnummer} fra PDL: $pdlFeil"
                )
            } else {
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
        hentFolkeregisterIdentRequest: HentFolkeregisterIdentRequest
    ): FolkeregisterIdent {
        logger.info("Henter folkeregisterident for ident=${hentFolkeregisterIdentRequest.ident} fra PDL")

        return pdlKlient.hentFolkeregisterIdent(hentFolkeregisterIdentRequest.ident).let {
            if (it.data?.hentIdenter == null) {
                val pdlFeil = it.errors?.joinToString(", ")
                throw PdlForesporselFeilet(
                    "Kunne ikke hente folkeregisterident for ident=${hentFolkeregisterIdentRequest.ident} " +
                        "fra PDL: $pdlFeil"
                )
            } else {
                try {
                    val folkeregisterIdent: String = it.data.hentIdenter.identer
                        .filter { it.gruppe == "FOLKEREGISTERIDENT" }
                        .first { !it.historisk }.ident
                    FolkeregisterIdent(folkeregisterident = Foedselsnummer.of(folkeregisterIdent))
                } catch (e: Exception) {
                    throw PdlForesporselFeilet(
                        "Fant ingen folkeregisterident for ident=${hentFolkeregisterIdentRequest.ident} fra PDL"
                    )
                }
            }
        }
    }
}