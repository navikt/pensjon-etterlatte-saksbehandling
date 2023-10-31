package no.nav.etterlatte.person

import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdl.FantIkkePersonException
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.GeografiskTilknytning
import no.nav.etterlatte.libs.common.person.HentFolkeregisterIdenterForAktoerIdBolkRequest
import no.nav.etterlatte.libs.common.person.HentGeografiskTilknytningRequest
import no.nav.etterlatte.libs.common.person.HentPdlIdentRequest
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.NavPersonIdent
import no.nav.etterlatte.libs.common.person.PDLIdentGruppeTyper
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.pdl.HistorikkForeldreansvar
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlKlient
import no.nav.etterlatte.pdl.PdlResponseError
import no.nav.etterlatte.pdl.mapper.ForeldreansvarHistorikkMapper
import no.nav.etterlatte.pdl.mapper.GeografiskTilknytningMapper
import no.nav.etterlatte.pdl.mapper.PersonMapper
import org.slf4j.LoggerFactory

class PdlForesporselFeilet(message: String) : RuntimeException(message)

enum class PersonMappingToggle(val key: String) : FeatureToggle {
    AksepterPersonerUtenIdenter("pensjon-etterlatte.aksepter-personer-uten-identer") {
        override fun key(): String {
            return key
        }
    },
}

class PersonService(
    private val pdlKlient: PdlKlient,
    private val ppsKlient: ParallelleSannheterKlient,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(PersonService::class.java)

    suspend fun hentPerson(request: HentPersonRequest): Person {
        logger.info("Henter person med fnr=${request.foedselsnummer} fra PDL")

        return pdlKlient.hentPerson(request).let {
            if (it.data?.hentPerson == null) {
                val pdlFeil = it.errors?.asFormatertFeil()
                if (it.errors?.personIkkeFunnet() == true) {
                    throw FantIkkePersonException("Fant ikke personen ${request.foedselsnummer}")
                } else {
                    throw PdlForesporselFeilet(
                        "Kunne ikke hente person med fnr=${request.foedselsnummer} fra PDL: $pdlFeil",
                    )
                }
            } else {
                PersonMapper.mapPerson(
                    ppsKlient = ppsKlient,
                    pdlKlient = pdlKlient,
                    fnr = request.foedselsnummer,
                    personRolle = request.rolle,
                    hentPerson = it.data.hentPerson,
                    saktype = request.saktype,
                    aksepterPersonerUtenIdent =
                        featureToggleService.isEnabled(
                            PersonMappingToggle.AksepterPersonerUtenIdenter,
                            false,
                        ),
                )
            }
        }
    }

    suspend fun hentHistorikkForeldreansvar(hentPersonRequest: HentPersonRequest): HistorikkForeldreansvar {
        if (hentPersonRequest.saktype != SakType.BARNEPENSJON) {
            throw IllegalArgumentException("Kan kun hente historikk i foreldreansvar for barnepensjonssaker")
        }
        if (hentPersonRequest.rolle != PersonRolle.BARN) {
            throw IllegalArgumentException("Kan kun hente historikk i foreldreansvar for barn")
        }
        val fnr = hentPersonRequest.foedselsnummer

        return pdlKlient.hentPersonHistorikkForeldreansvar(fnr)
            .let {
                if (it.data?.hentPerson == null) {
                    val pdlFeil = it.errors?.asFormatertFeil()
                    if (it.errors?.personIkkeFunnet() == true) {
                        throw FantIkkePersonException("Fant ikke personen $fnr")
                    } else {
                        throw PdlForesporselFeilet(
                            "Kunne ikke hente person med fnr=$fnr fra PDL: $pdlFeil",
                        )
                    }
                } else {
                    ForeldreansvarHistorikkMapper.mapForeldreAnsvar(it.data.hentPerson)
                }
            }
    }

    suspend fun hentOpplysningsperson(request: HentPersonRequest): PersonDTO {
        logger.info("Henter opplysninger for person med fnr=${request.foedselsnummer} fra PDL")

        return pdlKlient.hentPerson(request).let {
            if (it.data?.hentPerson == null) {
                val pdlFeil = it.errors?.asFormatertFeil()
                if (it.errors?.personIkkeFunnet() == true) {
                    throw FantIkkePersonException("Fant ikke personen ${request.foedselsnummer}")
                } else {
                    throw PdlForesporselFeilet(
                        "Kunne ikke hente opplysninger for ${request.foedselsnummer} fra PDL: $pdlFeil",
                    )
                }
            } else {
                PersonMapper.mapOpplysningsperson(
                    ppsKlient = ppsKlient,
                    pdlKlient = pdlKlient,
                    request = request,
                    hentPerson = it.data.hentPerson,
                    aksepterPersonerUtenIdent =
                        featureToggleService.isEnabled(
                            PersonMappingToggle.AksepterPersonerUtenIdenter,
                            false,
                        ),
                )
            }
        }
    }

    suspend fun hentPdlIdentifikator(request: HentPdlIdentRequest): PdlIdentifikator {
        logger.info("Henter pdlidentifikator for ident=${request.ident} fra PDL")

        return pdlKlient.hentPdlIdentifikator(request).let { identResponse ->
            if (identResponse.data?.hentIdenter == null) {
                val pdlFeil = identResponse.errors?.asFormatertFeil()
                if (identResponse.errors?.personIkkeFunnet() == true) {
                    throw FantIkkePersonException("Fant ikke personen ${request.ident}")
                } else {
                    throw PdlForesporselFeilet(
                        "Kunne ikke hente pdlidentifkator " +
                            "for ${request.ident} fra PDL: $pdlFeil",
                    )
                }
            } else {
                try {
                    val folkeregisterIdent: String? =
                        identResponse.data.hentIdenter.identer
                            .filter { it.gruppe == PDLIdentGruppeTyper.FOLKEREGISTERIDENT.navn }
                            .firstOrNull { !it.historisk }?.ident
                    if (folkeregisterIdent != null) {
                        PdlIdentifikator.FolkeregisterIdent(
                            folkeregisterident =
                                Folkeregisteridentifikator.of(
                                    folkeregisterIdent,
                                ),
                        )
                    } else {
                        val npid: String =
                            identResponse.data.hentIdenter.identer
                                .filter { it.gruppe == PDLIdentGruppeTyper.NPID.navn }
                                .first { !it.historisk }.ident
                        PdlIdentifikator.Npid(NavPersonIdent(npid))
                    }
                } catch (e: Exception) {
                    throw PdlForesporselFeilet(
                        "Fant ingen pdlidentifikator for ${request.ident} fra PDL",
                    )
                }
            }
        }
    }

    suspend fun hentFolkeregisterIdenterForAktoerIdBolk(request: HentFolkeregisterIdenterForAktoerIdBolkRequest): Map<String, String?> {
        logger.info("Henter folkeregisteridenter for aktørIds=${request.aktoerIds}")

        val response = pdlKlient.hentFolkeregisterIdenterForAktoerIdBolk(request)
        return response.associate { it.ident to it.identer.firstOrNull()?.ident }
    }

    suspend fun hentGeografiskTilknytning(request: HentGeografiskTilknytningRequest): GeografiskTilknytning {
        logger.info("Henter geografisk tilknytning med fnr=${request.foedselsnummer} fra PDL")

        return pdlKlient.hentGeografiskTilknytning(request).let {
            if (it.data?.hentGeografiskTilknytning == null) {
                val pdlFeil = it.errors?.asFormatertFeil()
                if (it.errors?.personIkkeFunnet() == true) {
                    throw FantIkkePersonException("Fant ikke personen ${request.foedselsnummer}")
                } else {
                    throw PdlForesporselFeilet(
                        "Kunne ikke hente fnr=${request.foedselsnummer} fra PDL: $pdlFeil",
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
