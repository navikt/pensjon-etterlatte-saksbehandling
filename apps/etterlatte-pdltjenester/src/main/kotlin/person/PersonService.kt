package no.nav.etterlatte.person

import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdl.AkseptererIkkePersonerUtenIdentException
import no.nav.etterlatte.libs.common.pdl.FantIkkePersonException
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.GeografiskTilknytning
import no.nav.etterlatte.libs.common.person.HentAdressebeskyttelseRequest
import no.nav.etterlatte.libs.common.person.HentFolkeregisterIdenterForAktoerIdBolkRequest
import no.nav.etterlatte.libs.common.person.HentGeografiskTilknytningRequest
import no.nav.etterlatte.libs.common.person.HentPdlIdentRequest
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.HentPersongalleriRequest
import no.nav.etterlatte.libs.common.person.NavPersonIdent
import no.nav.etterlatte.libs.common.person.PDLIdentGruppeTyper
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.person.hentPrioritertGradering
import no.nav.etterlatte.pdl.HistorikkForeldreansvar
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlKlient
import no.nav.etterlatte.pdl.PdlResponseError
import no.nav.etterlatte.pdl.mapper.ForeldreansvarHistorikkMapper
import no.nav.etterlatte.pdl.mapper.GeografiskTilknytningMapper
import no.nav.etterlatte.pdl.mapper.PersonMapper
import no.nav.etterlatte.sikkerLogg
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

    suspend fun hentAdressebeskyttelseGradering(request: HentAdressebeskyttelseRequest): AdressebeskyttelseGradering {
        logger.info("Henter person med fnr=${request.ident} fra PDL")

        return pdlKlient.hentAdressebeskyttelse(request).let {
            if (it.data?.hentPerson == null) {
                val pdlFeil = it.errors?.asFormatertFeil()
                if (it.errors?.personIkkeFunnet() == true) {
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

        if (persongalleri.personerUtenIdent.isNullOrEmpty() ||
            featureToggleService.isEnabled(
                PersonMappingToggle.AksepterPersonerUtenIdenter,
                false,
            )
        ) {
            return persongalleri
        }
        throw AkseptererIkkePersonerUtenIdentException()
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
                        saktype = SakType.BARNEPENSJON,
                    ),
            )
        val foreldre =
            mottaker.familieRelasjon?.ansvarligeForeldre?.map {
                hentPerson(
                    request =
                        HentPersonRequest(
                            foedselsnummer = it, rolle = PersonRolle.GJENLEVENDE, saktype = SakType.BARNEPENSJON,
                        ),
                )
            } ?: emptyList()

        val (avdoede, gjenlevende) = foreldre.partition { it.doedsdato != null }
        val soesken = avdoede.flatMap { it.avdoedesBarn ?: emptyList() }

        val alleTilknyttedePersonerUtenIdent =
            mottaker.familieRelasjon?.personerUtenIdent +
                avdoede.flatMap {
                    it.familieRelasjon?.personerUtenIdent ?: emptyList()
                }
        gjenlevende.flatMap { it.familieRelasjon?.personerUtenIdent ?: emptyList() }

        return Persongalleri(
            soeker = mottakerAvYtelsen.value,
            innsender = innsender?.value,
            soesken = soesken.map { it.foedselsnummer.value },
            avdoed = avdoede.map { it.foedselsnummer.value },
            gjenlevende = gjenlevende.map { it.foedselsnummer.value },
            personerUtenIdent = if (alleTilknyttedePersonerUtenIdent.isNullOrEmpty()) null else alleTilknyttedePersonerUtenIdent,
        )
    }

    suspend fun hentPersongalleriForOmstillingsstoenad(
        mottakerAvYtelsen: Folkeregisteridentifikator,
        innsender: Folkeregisteridentifikator?,
    ): Persongalleri {
        val mottaker =
            hentPerson(
                HentPersonRequest(
                    foedselsnummer = mottakerAvYtelsen,
                    rolle = PersonRolle.GJENLEVENDE,
                    saktype = SakType.OMSTILLINGSSTOENAD,
                ),
            )

        val partnerVedSivilstand =
            mottaker.sivilstand?.filter {
                listOf(
                    Sivilstatus.GIFT,
                    Sivilstatus.GJENLEVENDE_PARTNER,
                    Sivilstatus.ENKE_ELLER_ENKEMANN,
                ).contains(it.sivilstatus)
            }?.mapNotNull { it.relatertVedSiviltilstand } ?: emptyList()

        val (avdoede, levende) =
            partnerVedSivilstand.map {
                hentPerson(
                    HentPersonRequest(
                        foedselsnummer = it,
                        rolle = PersonRolle.GJENLEVENDE,
                        saktype = SakType.OMSTILLINGSSTOENAD,
                    ),
                )
            }.partition { it.doedsdato != null }

        // TODO: håndter tilfellet med felles barn med avdød riktig -- da gjelder det for samboer også

        val personerUtenIdent =
            (
                avdoede.flatMap {
                    it.familieRelasjon?.personerUtenIdent ?: emptyList()
                }
            ) + mottaker.familieRelasjon?.personerUtenIdent +
                levende.flatMap {
                    it.familieRelasjon?.personerUtenIdent ?: emptyList()
                }

        return Persongalleri(
            soeker = mottakerAvYtelsen.value,
            innsender = innsender?.value,
            soesken = listOf(),
            avdoed = avdoede.map { it.foedselsnummer.value },
            gjenlevende = listOf(mottakerAvYtelsen.value) + levende.map { it.foedselsnummer.value },
            personerUtenIdent = if (personerUtenIdent.isNullOrEmpty()) null else personerUtenIdent,
        )
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
                if (it.errors == null) {
                    logger.warn("Geografisk tilknytning er null i PDL (fnr=${request.foedselsnummer})")
                    sikkerLogg.warn("Geografisk tilknytning er null i PDL (fnr=${request.foedselsnummer.value})")

                    GeografiskTilknytning(ukjent = true)
                } else if (it.errors.personIkkeFunnet()) {
                    throw FantIkkePersonException("Fant ikke personen ${request.foedselsnummer}")
                } else {
                    val pdlFeil = it.errors.asFormatertFeil()
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

infix operator fun <T> List<T>?.plus(other: List<T>?): List<T> {
    return (this ?: emptyList()) + (other ?: emptyList())
}
