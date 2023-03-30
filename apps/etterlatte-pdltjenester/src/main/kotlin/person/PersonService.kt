package no.nav.etterlatte.person

import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.FolkeregisterIdent
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.GeografiskTilknytning
import no.nav.etterlatte.libs.common.person.HentFolkeregisterIdentRequest
import no.nav.etterlatte.libs.common.person.HentGeografiskTilknytningRequest
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.PDLIdentGruppeTyper
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlKlient
import no.nav.etterlatte.pdl.PdlResponseError
import no.nav.etterlatte.pdl.mapper.GeografiskTilknytningMapper
import no.nav.etterlatte.pdl.mapper.PersonMapper
import org.slf4j.LoggerFactory

class PdlForesporselFeilet(message: String) : RuntimeException(message)
class PdlFantIkkePerson(message: String) : RuntimeException(message)

class PersonService(
    private val pdlKlient: PdlKlient,
    private val ppsKlient: ParallelleSannheterKlient
) {
    private val logger = LoggerFactory.getLogger(PersonService::class.java)

    suspend fun hentPerson(request: HentPersonRequest): Person {
        logger.info("Henter person med fnr=${request.folkeregisteridentifikator} fra PDL")

        return pdlKlient.hentPerson(request.folkeregisteridentifikator, request.rolle).let {
            if (it.data?.hentPerson == null) {
                val pdlFeil = it.errors?.asFormatertFeil()
                if (it.errors?.personIkkeFunnet() == true) {
                    throw PdlFantIkkePerson("Fant ikke personen ${request.folkeregisteridentifikator}")
                } else {
                    throw PdlForesporselFeilet(
                        "Kunne ikke hente person med fnr=${request.folkeregisteridentifikator} fra PDL: $pdlFeil"
                    )
                }
            } else {
                PersonMapper.mapPerson(
                    ppsKlient = ppsKlient,
                    pdlKlient = pdlKlient,
                    fnr = request.folkeregisteridentifikator,
                    personRolle = request.rolle,
                    hentPerson = it.data.hentPerson
                )
            }
        }
    }

    suspend fun hentOpplysningsperson(request: HentPersonRequest): PersonDTO {
        logger.info("Henter opplysninger for person med fnr=${request.folkeregisteridentifikator} fra PDL")

        return pdlKlient.hentPerson(request.folkeregisteridentifikator, request.rolle).let {
            if (it.data?.hentPerson == null) {
                val pdlFeil = it.errors?.asFormatertFeil()
                if (it.errors?.personIkkeFunnet() == true) {
                    throw PdlFantIkkePerson("Fant ikke personen ${request.folkeregisteridentifikator}")
                } else {
                    throw PdlForesporselFeilet(
                        "Kunne ikke hente opplysninger for ${request.folkeregisteridentifikator} fra PDL: $pdlFeil"
                    )
                }
            } else {
                PersonMapper.mapOpplysningsperson(
                    ppsKlient = ppsKlient,
                    pdlKlient = pdlKlient,
                    fnr = request.folkeregisteridentifikator,
                    personRolle = request.rolle,
                    hentPerson = it.data.hentPerson
                )
            }
        }
    }

    suspend fun hentFolkeregisterIdent(request: HentFolkeregisterIdentRequest): FolkeregisterIdent {
        logger.info("Henter folkeregisterident for ident=${request.ident} fra PDL")

        return pdlKlient.hentFolkeregisterIdent(request.ident).let {
            if (it.data?.hentIdenter == null) {
                val pdlFeil = it.errors?.asFormatertFeil()
                if (it.errors?.personIkkeFunnet() == true) {
                    throw PdlFantIkkePerson("Fant ikke personen ${request.ident}")
                } else {
                    throw PdlForesporselFeilet(
                        "Kunne ikke hente folkeregisterident " +
                            "for ${request.ident} fra PDL: $pdlFeil"
                    )
                }
            } else {
                try {
                    val folkeregisterIdent: String = it.data.hentIdenter.identer
                        .filter { it.gruppe == PDLIdentGruppeTyper.FOLKEREGISTERIDENT.navn }
                        .first { !it.historisk }.ident
                    FolkeregisterIdent(folkeregisterident = Folkeregisteridentifikator.of(folkeregisterIdent))
                } catch (e: Exception) {
                    throw PdlForesporselFeilet(
                        "Fant ingen folkeregisterident for ${request.ident} fra PDL"
                    )
                }
            }
        }
    }

    suspend fun hentGeografiskTilknytning(request: HentGeografiskTilknytningRequest): GeografiskTilknytning {
        logger.info("Henter geografisk tilknytning med fnr=${request.folkeregisteridentifikator} fra PDL")

        return pdlKlient.hentGeografiskTilknytning(request.folkeregisteridentifikator).let {
            if (it.data?.hentGeografiskTilknytning == null) {
                val pdlFeil = it.errors?.asFormatertFeil()
                if (it.errors?.personIkkeFunnet() == true) {
                    throw PdlFantIkkePerson("Fant ikke personen ${request.folkeregisteridentifikator}")
                } else {
                    throw PdlForesporselFeilet(
                        "Kunne ikke hente fnr=${request.folkeregisteridentifikator} fra PDL: $pdlFeil"
                    )
                }
            } else {
                GeografiskTilknytningMapper.mapGeografiskTilknytning(it.data.hentGeografiskTilknytning)
            }
        }
    }

    fun List<PdlResponseError>.asFormatertFeil() = this.joinToString(", ")
    fun List<PdlResponseError>.personIkkeFunnet() = any { it.extensions?.code == "not_found" }
}