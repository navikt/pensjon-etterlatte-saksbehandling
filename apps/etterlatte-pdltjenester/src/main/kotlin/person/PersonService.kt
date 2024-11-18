package no.nav.etterlatte.person

import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdl.FantIkkePersonException
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.GeografiskTilknytning
import no.nav.etterlatte.libs.common.person.HentAdressebeskyttelseRequest
import no.nav.etterlatte.libs.common.person.HentGeografiskTilknytningRequest
import no.nav.etterlatte.libs.common.person.HentPdlIdentRequest
import no.nav.etterlatte.libs.common.person.HentPersonHistorikkForeldreAnsvarRequest
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.HentPersongalleriRequest
import no.nav.etterlatte.libs.common.person.NavPersonIdent
import no.nav.etterlatte.libs.common.person.PDLIdentGruppeTyper
import no.nav.etterlatte.libs.common.person.PdlFolkeregisterIdentListe
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.person.hentPrioritertGradering
import no.nav.etterlatte.pdl.HistorikkForeldreansvar
import no.nav.etterlatte.pdl.PdlKlient
import no.nav.etterlatte.pdl.PdlResponseError
import no.nav.etterlatte.pdl.mapper.ForeldreansvarHistorikkMapper
import no.nav.etterlatte.pdl.mapper.GeografiskTilknytningMapper
import no.nav.etterlatte.pdl.mapper.ParallelleSannheterService
import no.nav.etterlatte.sikkerLogg
import org.slf4j.LoggerFactory

class PdlForesporselFeilet(
    message: String,
) : RuntimeException(message)

class PersonService(
    private val pdlKlient: PdlKlient,
    private val parallelleSannheterService: ParallelleSannheterService,
) {
    private val logger = LoggerFactory.getLogger(PersonService::class.java)

    suspend fun hentOpplysningsperson(request: HentPersonRequest): PersonDTO {
        logger.info("Henter opplysninger for person med fnr=${request.foedselsnummer} fra PDL")

        return pdlKlient.hentPerson(request).let {
            if (it.data?.hentPerson == null) {
                val pdlFeil = it.errors?.asFormatertFeil()
                if (it.errors?.harAdressebeskyttelse() == true) {
                    throw pdlForesporselFeiletForAdressebeskyttelse()
                } else if (it.errors?.personIkkeFunnet() == true) {
                    throw FantIkkePersonException("Fant ikke personen ${request.foedselsnummer}")
                } else {
                    throw PdlForesporselFeilet(
                        "Kunne ikke hente opplysninger for ${request.foedselsnummer} fra PDL: $pdlFeil",
                    )
                }
            } else {
                parallelleSannheterService.mapOpplysningsperson(
                    request = request,
                    hentPerson = it.data.hentPerson,
                )
            }
        }
    }

    suspend fun hentPerson(request: HentPersonRequest): Person {
        logger.info("Henter person med fnr=${request.foedselsnummer} fra PDL")

        return pdlKlient.hentPerson(request).let {
            if (it.data?.hentPerson == null) {
                val pdlFeil = it.errors?.asFormatertFeil()
                if (it.errors?.harAdressebeskyttelse() == true) {
                    throw pdlForesporselFeiletForAdressebeskyttelse()
                } else if (it.errors?.personIkkeFunnet() == true) {
                    throw FantIkkePersonException("Fant ikke personen ${request.foedselsnummer}")
                } else {
                    throw PdlForesporselFeilet(
                        "Kunne ikke hente person med fnr=${request.foedselsnummer} fra PDL: $pdlFeil",
                    )
                }
            } else {
                // TODO: bruke mapOpplysningsperson også PersonDTO toPerson?
                parallelleSannheterService.mapPerson(
                    oppslagFnr = request.foedselsnummer,
                    personRolle = request.rolle,
                    hentPerson = it.data.hentPerson,
                    saktyper = request.saktyper,
                )
            }
        }
    }

    suspend fun hentAdressebeskyttelseGradering(request: HentAdressebeskyttelseRequest): AdressebeskyttelseGradering {
        logger.info("Henter adressebeskyttelse for person med fnr=${request.ident} fra PDL")

        return pdlKlient.hentAdressebeskyttelse(request).let {
            if (it.data?.hentPerson == null) {
                val pdlFeil = it.errors?.asFormatertFeil()
                if (it.errors?.harAdressebeskyttelse() == true) {
                    throw pdlForesporselFeiletForAdressebeskyttelse()
                } else if (it.errors?.personIkkeFunnet() == true) {
                    throw FantIkkePersonException("Fant ikke personen ${request.ident}")
                } else {
                    throw PdlForesporselFeilet(
                        "Kunne ikke hente person med fnr=${request.ident} fra PDL: $pdlFeil",
                    )
                }
            } else {
                it.data.hentPerson.adressebeskyttelse
                    .mapNotNull { adr -> adr.gradering }
                    .map { pdlGradering -> AdressebeskyttelseGradering.valueOf(pdlGradering.name) }
                    .hentPrioritertGradering()
            }
        }
    }

    suspend fun hentHistorikkForeldreansvar(hentPersonRequest: HentPersonHistorikkForeldreAnsvarRequest): HistorikkForeldreansvar {
        if (hentPersonRequest.saktype != SakType.BARNEPENSJON) {
            throw IllegalArgumentException("Kan kun hente historikk i foreldreansvar for barnepensjonssaker")
        }
        if (hentPersonRequest.rolle != PersonRolle.BARN) {
            throw IllegalArgumentException("Kan kun hente historikk i foreldreansvar for barn")
        }
        val fnr = hentPersonRequest.foedselsnummer

        return pdlKlient
            .hentPersonHistorikkForeldreansvar(fnr)
            .let {
                if (it.data?.hentPerson == null) {
                    val pdlFeil = it.errors?.asFormatertFeil()
                    if (it.errors?.harAdressebeskyttelse() == true) {
                        throw pdlForesporselFeiletForAdressebeskyttelse()
                    } else if (it.errors?.personIkkeFunnet() == true) {
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
                            .firstOrNull { !it.historisk }
                            ?.ident
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
                                .first { !it.historisk }
                                .ident
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

    suspend fun hentPdlFolkeregisterIdenter(request: HentPdlIdentRequest): PdlFolkeregisterIdentListe {
        logger.info("Henter alle folkeregisteridenter for ident=${request.ident} fra PDL, inkl. historiske")

        val response = pdlKlient.hentPdlIdentifikator(request, listOf(PDLIdentGruppeTyper.FOLKEREGISTERIDENT))

        return if (response.data?.hentIdenter == null) {
            val pdlFeil = response.errors?.asFormatertFeil()

            if (response.errors?.personIkkeFunnet() == true) {
                throw FantIkkePersonException("Fant ikke personen ${request.ident}")
            } else {
                throw PdlForesporselFeilet(
                    "Kunne ikke hente pdlidentifkator for ${request.ident} fra PDL: $pdlFeil",
                )
            }
        } else {
            response.data.hentIdenter.identer
                .filterNot { it.gruppe == PDLIdentGruppeTyper.FOLKEREGISTERIDENT.navn }
                .map {
                    PdlIdentifikator.FolkeregisterIdent(
                        Folkeregisteridentifikator.of(it.ident),
                        it.historisk,
                    )
                }.let(::PdlFolkeregisterIdentListe)
        }
    }

    suspend fun hentPersongalleri(hentPersongalleriRequest: HentPersongalleriRequest): Persongalleri {
        val persongalleri =
            when (hentPersongalleriRequest.saktype) {
                SakType.BARNEPENSJON ->
                    hentPersongalleriForBarnepensjon(
                        hentPersongalleriRequest.mottakerAvYtelsen,
                        hentPersongalleriRequest.innsender,
                    )

                SakType.OMSTILLINGSSTOENAD ->
                    hentPersongalleriForOmstillingsstoenad(
                        hentPersongalleriRequest.mottakerAvYtelsen,
                        hentPersongalleriRequest.innsender,
                    )
            }

        return persongalleri
    }

    private suspend fun hentPersongalleriForBarnepensjon(
        mottakerAvYtelsen: Folkeregisteridentifikator,
        innsender: Folkeregisteridentifikator?,
    ): Persongalleri {
        val mottaker =
            hentPerson(
                request =
                    HentPersonRequest(
                        foedselsnummer = mottakerAvYtelsen,
                        rolle = PersonRolle.BARN,
                        saktyper = listOf(SakType.BARNEPENSJON),
                    ),
            )
        val foreldre =
            mottaker.familieRelasjon?.foreldre?.map {
                hentPerson(
                    request =
                        HentPersonRequest(
                            foedselsnummer = it,
                            rolle = PersonRolle.GJENLEVENDE,
                            saktyper = listOf(SakType.BARNEPENSJON),
                        ),
                )
            } ?: emptyList()

        val (avdoede, gjenlevende) = foreldre.partition { it.doedsdato != null }
        val soesken = avdoede.flatMap { it.avdoedesBarn ?: emptyList() }

        val alleTilknyttedePersonerUtenIdent =
            mottaker.familieRelasjon
                ?.personerUtenIdent
                ?.plus(
                    avdoede.flatMap {
                        it.familieRelasjon?.personerUtenIdent ?: emptyList()
                    },
                )?.plus(gjenlevende.flatMap { it.familieRelasjon?.personerUtenIdent ?: emptyList() })

        return Persongalleri(
            soeker = mottaker.foedselsnummer.value,
            innsender = innsender?.value,
            soesken = soesken.map { it.foedselsnummer.value },
            avdoed = avdoede.map { it.foedselsnummer.value },
            gjenlevende = gjenlevende.map { it.foedselsnummer.value },
            personerUtenIdent = if (alleTilknyttedePersonerUtenIdent.isNullOrEmpty()) null else alleTilknyttedePersonerUtenIdent,
        )
    }

    private suspend fun hentPersongalleriForOmstillingsstoenad(
        mottakerAvYtelsen: Folkeregisteridentifikator,
        innsender: Folkeregisteridentifikator?,
    ): Persongalleri {
        val mottaker =
            hentPerson(
                HentPersonRequest(
                    foedselsnummer = mottakerAvYtelsen,
                    rolle = PersonRolle.GJENLEVENDE,
                    saktyper = listOf(SakType.BARNEPENSJON),
                ),
            )

        val partnerVedSivilstand =
            mottaker.sivilstand
                ?.filter {
                    listOf(
                        Sivilstatus.GIFT,
                        Sivilstatus.GJENLEVENDE_PARTNER,
                        Sivilstatus.ENKE_ELLER_ENKEMANN,
                    ).contains(it.sivilstatus)
                }?.mapNotNull { it.relatertVedSiviltilstand } ?: emptyList()

        val (avdoede, levende) =
            partnerVedSivilstand
                .map {
                    hentPerson(
                        HentPersonRequest(
                            foedselsnummer = it,
                            rolle = PersonRolle.GJENLEVENDE,
                            saktyper = listOf(SakType.OMSTILLINGSSTOENAD),
                        ),
                    )
                }.partition { it.doedsdato != null }

        // TODO: håndter tilfellet med felles barn med avdød riktig -- da gjelder det for samboer også
        val personerUtenIdent =
            (
                avdoede.flatMap {
                    it.familieRelasjon?.personerUtenIdent ?: emptyList()
                }
            ).plus(mottaker.familieRelasjon?.personerUtenIdent ?: emptyList()).plus(
                levende.flatMap {
                    it.familieRelasjon?.personerUtenIdent ?: emptyList()
                },
            )

        return Persongalleri(
            soeker = mottaker.foedselsnummer.value,
            innsender = innsender?.value,
            soesken = listOf(),
            avdoed = avdoede.map { it.foedselsnummer.value },
            gjenlevende = listOf(mottaker.foedselsnummer.value) + levende.map { it.foedselsnummer.value },
            personerUtenIdent = personerUtenIdent.ifEmpty { null },
        )
    }

    suspend fun hentGeografiskTilknytning(request: HentGeografiskTilknytningRequest): GeografiskTilknytning {
        logger.info("Henter geografisk tilknytning med fnr=${request.foedselsnummer} fra PDL")

        return pdlKlient.hentGeografiskTilknytning(request).let {
            val geografiskTilknytning = it.data?.hentGeografiskTilknytning

            if (geografiskTilknytning == null) {
                if (it.errors == null) {
                    logger.warn("Geografisk tilknytning er null i PDL (fnr=${request.foedselsnummer})")
                    sikkerLogg.warn("Geografisk tilknytning er null i PDL (fnr=${request.foedselsnummer.value})")

                    GeografiskTilknytning(ukjent = true)
                } else if (it.errors?.harAdressebeskyttelse() == true) {
                    throw pdlForesporselFeiletForAdressebeskyttelse()
                } else if (it.errors.personIkkeFunnet()) {
                    throw FantIkkePersonException("Fant ikke personen ${request.foedselsnummer}")
                } else {
                    val pdlFeil = it.errors.asFormatertFeil()
                    throw PdlForesporselFeilet(
                        "Kunne ikke hente fnr=${request.foedselsnummer} fra PDL: $pdlFeil",
                    )
                }
            } else {
                logger.info("Fant geografisk tilknytning: $geografiskTilknytning")
                GeografiskTilknytningMapper.mapGeografiskTilknytning(geografiskTilknytning)
            }
        }
    }

    suspend fun hentAktoerId(request: HentPdlIdentRequest): PdlIdentifikator.AktoerId =
        pdlKlient.hentAktoerId(request).let { res ->
            if (res.data?.hentIdenter?.identer == null) {
                val pdlFeil = res.errors?.asFormatertFeil()
                if (res.errors?.harAdressebeskyttelse() == true) {
                    throw pdlForesporselFeiletForAdressebeskyttelse()
                } else if (res.errors?.personIkkeFunnet() == true) {
                    throw FantIkkePersonException("Fant ikke personen ${request.ident}")
                } else {
                    throw PdlForesporselFeilet(
                        "Kunne ikke hente aktørid for ${request.ident} fra PDL: $pdlFeil",
                    )
                }
            } else {
                res.data.hentIdenter.identer
                    .first { it.gruppe == PDLIdentGruppeTyper.AKTORID.navn && !it.historisk }
                    .let { PdlIdentifikator.AktoerId(it.ident) }
            }
        }

    fun List<PdlResponseError>.asFormatertFeil() = this.joinToString(", ")

    fun List<PdlResponseError>.personIkkeFunnet() = any { it.extensions?.code == "not_found" }

    private fun List<PdlResponseError>.harAdressebeskyttelse() =
        any { error ->
            error.extensions?.code == "unauthorized" &&
                error.extensions
                    ?.details
                    ?.policy
                    ?.let { policy ->
                        policy.contains("adressebeskyttelse_fortrolig_adresse") ||
                            policy.contains("adressebeskyttelse_strengt_fortrolig_adresse")
                    } == true
        }

    private fun pdlForesporselFeiletForAdressebeskyttelse(): Throwable =
        throw no.nav.etterlatte.personweb.PdlForesporselFeilet(
            "Denne personen har adressebeskyttelse. Behandlingen skal derfor sendes til enhet Vikafossen som vil behandle saken videre.",
        )
}
