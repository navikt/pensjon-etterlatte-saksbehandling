package no.nav.etterlatte.pdl.mapper

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.PersonUtenIdent
import no.nav.etterlatte.libs.common.behandling.RelativPersonrolle
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.pdl.PersonDoedshendelseDto
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.person.Statsborgerskap
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.pdl.ParallelleSannheterException
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlFoedselsdato
import no.nav.etterlatte.pdl.PdlForelderBarnRelasjonRolle
import no.nav.etterlatte.pdl.PdlHentFoedselsdato
import no.nav.etterlatte.pdl.PdlHentPerson
import no.nav.etterlatte.pdl.PdlHentPersonNavnFoedselsdato
import no.nav.etterlatte.pdl.PdlKlient
import no.nav.etterlatte.pdl.PdlOboKlient
import no.nav.etterlatte.pdl.PdlSivilstand
import no.nav.etterlatte.pdl.PdlStatsborgerskap
import no.nav.etterlatte.pdl.SoekPersonTreff
import no.nav.etterlatte.personweb.dto.PersonNavnFoedselsaar
import no.nav.etterlatte.personweb.dto.PersonSoekSvar
import no.nav.etterlatte.personweb.familieOpplysninger.Bostedsadresse
import no.nav.etterlatte.personweb.familieOpplysninger.Familiemedlem
import no.nav.etterlatte.personweb.familieOpplysninger.Familierelasjon
import no.nav.etterlatte.personweb.familieOpplysninger.Sivilstand
import no.nav.etterlatte.sikkerLogg
import org.slf4j.LoggerFactory
import java.util.UUID

class ParallelleSannheterService(
    private val ppsKlient: ParallelleSannheterKlient,
    private val pdlKlient: PdlKlient,
    private val pdlOboKlient: PdlOboKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun mapPerson(
        oppslagFnr: Folkeregisteridentifikator,
        personRolle: PersonRolle,
        hentPerson: PdlHentPerson,
        saktyper: List<SakType>,
    ): Person =
        runBlocking {
            val fnr =
                if (hentPerson.folkeregisteridentifikator == null) {
                    if (personRolle != PersonRolle.TILKNYTTET_BARN) {
                        logger.warn(
                            "Fikk person som mangler folkeregisteridentifikator i PDL. Se sikkerlogg for fnr som oppslaget ble utført med.",
                        )
                        sikkerLogg.warn(
                            "Person med fnr=${oppslagFnr.value} og rolle=$personRolle mangler folkeregisteridentifikator i PDL",
                        )
                    }
                    oppslagFnr
                } else {
                    ppsKlient
                        .avklarFolkeregisteridentifikator(hentPerson.folkeregisteridentifikator)
                        .let { Folkeregisteridentifikator.of(it.identifikasjonsnummer) }
                }

            val navn = ppsKlient.avklarNavn(hentPerson.navn)
            val adressebeskyttelse = ppsKlient.avklarAdressebeskyttelse(hentPerson.adressebeskyttelse)
            val (statsborgerskap, pdlStatsborgerskap) =
                kanskjeAvklarStatsborgerskap(hentPerson.statsborgerskap)
            val sivilstand =
                hentPerson.sivilstand?.let { pdlSivilstand -> avklarSivilstatus(fnr, pdlSivilstand) }
            val foedselsdato = ppsKlient.avklarFoedselsdato(hentPerson.foedselsdato)
            val foedested = ppsKlient.avklarFoedested(hentPerson.foedested)
            val doedsfall = ppsKlient.avklarDoedsfall(hentPerson.doedsfall)
            val barnekull =
                if (personRolle == PersonRolle.AVDOED) {
                    mapBarnekull(
                        hentPerson,
                        saktyper,
                    )
                } else {
                    null
                }

            Person(
                fornavn = navn.fornavn,
                mellomnavn = navn.mellomnavn,
                etternavn = navn.etternavn,
                foedselsnummer = fnr,
                foedselsdato = foedselsdato.foedselsdato,
                foedselsaar = foedselsdato.foedselsaar,
                doedsdato = doedsfall?.doedsdato,
                foedeland = foedested?.foedeland,
                adressebeskyttelse =
                    adressebeskyttelse?.let {
                        AdressebeskyttelseGradering.valueOf(it.gradering.toString())
                    }
                        ?: AdressebeskyttelseGradering.UGRADERT,
                bostedsadresse = hentPerson.bostedsadresse?.let { AdresseMapper.mapBostedsadresse(ppsKlient, it) },
                oppholdsadresse = hentPerson.oppholdsadresse?.let { AdresseMapper.mapOppholdsadresse(ppsKlient, it) },
                deltBostedsadresse =
                    hentPerson.deltBostedsadresse?.let {
                        AdresseMapper.mapDeltBostedsadresse(ppsKlient, it)
                    },
                kontaktadresse = hentPerson.kontaktadresse?.let { AdresseMapper.mapKontaktadresse(ppsKlient, it) },
                statsborgerskap = statsborgerskap?.land,
                pdlStatsborgerskap = pdlStatsborgerskap,
                sivilstatus = sivilstand?.verdi ?: Sivilstatus.UOPPGITT,
                sivilstand = hentPerson.sivilstand?.let { SivilstandMapper.mapSivilstand(it) },
                utland = UtlandMapper.mapUtland(hentPerson),
                familieRelasjon =
                    FamilieRelasjonMapper.mapFamilieRelasjon(
                        hentPerson,
                        personRolle,
                    ),
                avdoedesBarn = barnekull?.barn,
                avdoedesBarnUtenIdent = barnekull?.barnUtenIdent,
                vergemaalEllerFremtidsfullmakt = VergeMapper.mapVerge(hentPerson),
            )
        }

    fun mapFamiliemedlem(
        hentPerson: PdlHentPerson,
        ident: Folkeregisteridentifikator,
        sakType: SakType,
        brukerTokenInfo: BrukerTokenInfo,
        personRolle: PersonRolle,
    ): Familiemedlem =
        runBlocking {
            val navn = ppsKlient.avklarNavn(hentPerson.navn)
            val (statsborgerskap, pdlStatsborgerskap) =
                kanskjeAvklarStatsborgerskap(
                    hentPerson.statsborgerskap,
                )
            val sivilstand = hentPerson.sivilstand?.let { SivilstandMapper.mapSivilstand(it) }
            val foedselsdato = ppsKlient.avklarFoedselsdato(hentPerson.foedselsdato)
            val doedsfall = ppsKlient.avklarDoedsfall(hentPerson.doedsfall)
            val bostedsadresse = hentPerson.bostedsadresse?.let { AdresseMapper.mapBostedsadresse(ppsKlient, it) }
            val barnekull =
                mapBarnekullPersonopplysning(
                    forelder = hentPerson,
                    sakType = sakType,
                    brukerTokenInfo = brukerTokenInfo,
                )

            Familiemedlem(
                fornavn = navn.fornavn,
                etternavn = navn.etternavn,
                foedselsnummer = ident,
                foedselsdato = foedselsdato.foedselsdato,
                doedsdato = doedsfall?.doedsdato,
                bostedsadresse =
                    bostedsadresse?.map {
                        Bostedsadresse(it.adresseLinje1, it.postnr, it.gyldigFraOgMed, it.gyldigTilOgMed, it.aktiv)
                    },
                statsborgerskap = statsborgerskap?.land,
                pdlStatsborgerskap = pdlStatsborgerskap,
                sivilstand =
                    sivilstand?.map {
                        Sivilstand(it.sivilstatus, it.relatertVedSiviltilstand, it.gyldigFraOgMed)
                    },
                utland = UtlandMapper.mapUtland(hentPerson),
                barn = barnekull?.barn,
                vergemaalEllerFremtidsfullmakt = VergeMapper.mapVerge(hentPerson),
                familierelasjon =
                    FamilieRelasjonMapper
                        .mapFamilieRelasjon(
                            hentPerson,
                            personRolle,
                        ).let {
                            Familierelasjon(
                                ansvarligeForeldre = it.ansvarligeForeldre,
                                foreldre = it.foreldre,
                                barn = it.barn,
                            )
                        },
            )
        }

    fun mapPersonNavnFoedsel(hentPerson: PdlHentPersonNavnFoedselsdato): PersonNavnFoedselsaar =
        runBlocking {
            val navn = ppsKlient.avklarNavn(hentPerson.navn)

            val fnr =
                ppsKlient
                    .avklarFolkeregisteridentifikator(hentPerson.folkeregisteridentifikator)
                    .identifikasjonsnummer

            val historiskeFoedselsnummer =
                hentPerson.folkeregisteridentifikator
                    .filter { it.metadata.historisk }
                    .map { Folkeregisteridentifikator.of(it.identifikasjonsnummer) }

            val foedselsdato = ppsKlient.avklarFoedselsdato(hentPerson.foedselsdato)
            val doedsfall = ppsKlient.avklarDoedsfall(hentPerson.doedsfall)

            PersonNavnFoedselsaar(
                fornavn = navn.fornavn,
                mellomnavn = navn.mellomnavn,
                etternavn = navn.etternavn,
                foedselsnummer = Folkeregisteridentifikator.of(fnr),
                historiskeFoedselsnummer = historiskeFoedselsnummer,
                foedselsdato = foedselsdato.foedselsdato,
                foedselsaar = foedselsdato.foedselsaar,
                doedsdato = doedsfall?.doedsdato,
                // PPS støtter p.t. ikke verge. Henter derfor ut første fra listen hvis den ikke er null/tom
                vergemaal = hentPerson.vergemaalEllerFremtidsfullmakt?.firstNotNullOfOrNull { VergeMapper.mapVerge(it) },
            )
        }

    suspend fun mapFoedselsdato(hentFoedselsdato: PdlHentFoedselsdato): PdlFoedselsdato =
        ppsKlient.avklarFoedselsdato(hentFoedselsdato.foedselsdato)

    suspend fun mapPersonSoek(
        ident: String,
        soekPerson: SoekPersonTreff,
    ): PersonSoekSvar {
        val navn = ppsKlient.avklarNavn(soekPerson.navn)
        val fnr =
            if (Folkeregisteridentifikator.isValid(ident)) {
                ident
            } else {
                ppsKlient
                    .avklarFolkeregisteridentifikator(soekPerson.folkeregisteridentifikator)
                    .identifikasjonsnummer
            }

        return PersonSoekSvar(
            fornavn = navn.fornavn,
            mellomnavn = navn.mellomnavn,
            etternavn = navn.etternavn,
            foedselsnummer = fnr,
            bostedsadresse = soekPerson.bostedsadresse?.let { AdresseMapper.mapBostedsadresse(ppsKlient, it) },
        )
    }

    fun mapOpplysningsperson(
        hentPerson: PdlHentPerson,
        request: HentPersonRequest,
    ): PersonDTO =
        runBlocking {
            val folkeregisteridentifikator =
                if (hentPerson.folkeregisteridentifikator.isNullOrEmpty()) {
                    logger.warn(
                        "Fikk person som mangler folkeregisteridentifikator i PDL. Se sikkerlogg for fnr som oppslaget ble utført med.",
                    )
                    sikkerLogg.warn(
                        "Person med fnr=${request.foedselsnummer} og mangler folkeregisteridentifikator i PDL",
                    )
                    request.foedselsnummer
                } else {
                    ppsKlient
                        .avklarFolkeregisteridentifikator(hentPerson.folkeregisteridentifikator)
                        .let { Folkeregisteridentifikator.of(it.identifikasjonsnummer) }
                }

            val navn = ppsKlient.avklarNavn(hentPerson.navn)
            val adressebeskyttelse = ppsKlient.avklarAdressebeskyttelse(hentPerson.adressebeskyttelse)
            val (statsborgerskap, statsborgerskapPdl) =
                kanskjeAvklarStatsborgerskap(
                    hentPerson.statsborgerskap,
                )
            val sivilstand =
                hentPerson.sivilstand?.let { pdlSivilstand ->
                    avklarSivilstatus(request.foedselsnummer, pdlSivilstand)
                }
            val foedselsdato = ppsKlient.avklarFoedselsdato(hentPerson.foedselsdato)
            val foedested = ppsKlient.avklarFoedested(hentPerson.foedested)
            val doedsfall = ppsKlient.avklarDoedsfall(hentPerson.doedsfall)
            val barnekull =
                if (request.rolle == PersonRolle.AVDOED) {
                    mapBarnekull(
                        hentPerson,
                        request.saktyper,
                    )
                } else {
                    null
                }

            PersonDTO(
                fornavn = OpplysningDTO(navn.fornavn, navn.metadata.opplysningsId),
                mellomnavn = navn.mellomnavn?.let { OpplysningDTO(navn.mellomnavn, navn.metadata.opplysningsId) },
                etternavn = OpplysningDTO(navn.etternavn, navn.metadata.opplysningsId),
                foedselsnummer = OpplysningDTO(folkeregisteridentifikator, null),
                foedselsdato =
                    foedselsdato.foedselsdato?.let {
                        OpplysningDTO(
                            it,
                            foedselsdato.metadata.opplysningsId,
                        )
                    },
                foedselsaar = OpplysningDTO(foedselsdato.foedselsaar, foedselsdato.metadata.opplysningsId),
                doedsdato = doedsfall?.doedsdato?.let { OpplysningDTO(it, doedsfall.metadata.opplysningsId) },
                foedeland = foedested?.foedeland?.let { OpplysningDTO(it, foedested.metadata.opplysningsId) },
                adressebeskyttelse =
                    adressebeskyttelse?.let {
                        OpplysningDTO(
                            AdressebeskyttelseGradering.valueOf(it.gradering.toString()),
                            it.metadata.opplysningsId,
                        )
                    } ?: OpplysningDTO(AdressebeskyttelseGradering.UGRADERT, null),
                // Finn ut hva opplysningsid:n er for data fra pps
                bostedsadresse =
                    hentPerson.bostedsadresse
                        ?.let { AdresseMapper.mapBostedsadresse(ppsKlient, it) }
                        ?.map { OpplysningDTO(it, null) },
                oppholdsadresse =
                    hentPerson.oppholdsadresse
                        ?.let { AdresseMapper.mapOppholdsadresse(ppsKlient, it) }
                        ?.map { OpplysningDTO(it, null) },
                deltBostedsadresse =
                    hentPerson.deltBostedsadresse
                        ?.let {
                            AdresseMapper.mapDeltBostedsadresse(ppsKlient, it)
                        }?.map { OpplysningDTO(it, null) },
                kontaktadresse =
                    hentPerson.kontaktadresse
                        ?.let { AdresseMapper.mapKontaktadresse(ppsKlient, it) }
                        ?.map { OpplysningDTO(it, null) },
                statsborgerskap = statsborgerskap?.let { OpplysningDTO(it.land, it.metadata.opplysningsId) },
                pdlStatsborgerskap = statsborgerskapPdl?.let { OpplysningDTO(it, null) },
                sivilstatus = sivilstand,
                sivilstand =
                    hentPerson.sivilstand
                        ?.let { SivilstandMapper.mapSivilstand(it) }
                        ?.map { OpplysningDTO(it, null) },
                utland = OpplysningDTO(UtlandMapper.mapUtland(hentPerson), null),
                // TODO ai: tre opplysninger i en
                familieRelasjon =
                    OpplysningDTO(
                        FamilieRelasjonMapper.mapFamilieRelasjon(hentPerson, request.rolle),
                        null,
                    ),
                avdoedesBarn = barnekull?.barn,
                vergemaalEllerFremtidsfullmakt =
                    VergeMapper
                        .mapVerge(hentPerson)
                        ?.map { OpplysningDTO(it, null) },
            )
        }

    suspend fun mapDoedshendelsePerson(
        request: HentPersonRequest,
        hentPerson: PdlHentPerson,
    ): PersonDoedshendelseDto {
        val folkeregisteridentifikator =
            if (hentPerson.folkeregisteridentifikator.isNullOrEmpty()) {
                logger.warn(
                    "Fikk person som mangler folkeregisteridentifikator i PDL. Se sikkerlogg for fnr som oppslaget ble utført med.",
                )
                sikkerLogg.warn(
                    "Person med fnr=${request.foedselsnummer} og mangler folkeregisteridentifikator i PDL",
                )
                request.foedselsnummer
            } else {
                ppsKlient
                    .avklarFolkeregisteridentifikator(hentPerson.folkeregisteridentifikator)
                    .let { Folkeregisteridentifikator.of(it.identifikasjonsnummer) }
            }

        val foedselsdato = ppsKlient.avklarNullableFoedselsdato(hentPerson.foedselsdato)
        val doedsfall = ppsKlient.avklarDoedsfall(hentPerson.doedsfall)
        val barnekull =
            if (request.rolle == PersonRolle.AVDOED) {
                mapBarnekull(
                    hentPerson,
                    request.saktyper,
                )
            } else {
                null
            }

        return PersonDoedshendelseDto(
            foedselsnummer = OpplysningDTO(folkeregisteridentifikator, null),
            foedselsdato =
                foedselsdato?.foedselsdato?.let {
                    OpplysningDTO(
                        it,
                        foedselsdato.metadata.opplysningsId,
                    )
                },
            foedselsaar =
                foedselsdato?.let {
                    OpplysningDTO(
                        foedselsdato.foedselsaar,
                        foedselsdato.metadata.opplysningsId,
                    )
                },
            doedsdato = doedsfall?.doedsdato?.let { OpplysningDTO(it, doedsfall.metadata.opplysningsId) },
            bostedsadresse =
                hentPerson.bostedsadresse
                    ?.let { AdresseMapper.mapBostedsadresse(ppsKlient, it) }
                    ?.map { OpplysningDTO(it, null) },
            oppholdsadresse =
                hentPerson.oppholdsadresse
                    ?.let { AdresseMapper.mapOppholdsadresse(ppsKlient, it) }
                    ?.map { OpplysningDTO(it, null) },
            deltBostedsadresse =
                hentPerson.deltBostedsadresse
                    ?.let {
                        AdresseMapper.mapDeltBostedsadresse(ppsKlient, it)
                    }?.map { OpplysningDTO(it, null) },
            kontaktadresse =
                hentPerson.kontaktadresse
                    ?.let { AdresseMapper.mapKontaktadresse(ppsKlient, it) }
                    ?.map { OpplysningDTO(it, null) },
            sivilstand =
                hentPerson.sivilstand
                    ?.let { SivilstandMapper.mapSivilstand(it) }
                    ?.map { OpplysningDTO(it, null) },
            utland = OpplysningDTO(UtlandMapper.mapUtland(hentPerson), null),
            familieRelasjon =
                OpplysningDTO(
                    FamilieRelasjonMapper.mapFamilieRelasjon(hentPerson, request.rolle),
                    null,
                ),
            avdoedesBarn = barnekull?.barn,
            avdoedesBarnUtenIdent = barnekull?.barnUtenIdent,
        )
    }

    private suspend fun mapBarnekull(
        forelder: PdlHentPerson,
        saktyper: List<SakType>,
    ): Barnekull? {
        val barnFnr =
            forelder.forelderBarnRelasjon
                ?.filter {
                    it.relatertPersonsRolle == PdlForelderBarnRelasjonRolle.BARN &&
                        it.relatertPersonsIdent != null
                }?.map { it.relatertPersonsIdent }
                ?.distinct()
                ?.map { Folkeregisteridentifikator.of(it) }

        val personerUtenIdent =
            forelder.forelderBarnRelasjon
                ?.filter {
                    it.relatertPersonsRolle == PdlForelderBarnRelasjonRolle.BARN &&
                        it.relatertPersonUtenFolkeregisteridentifikator != null
                }?.map {
                    PersonUtenIdent(
                        RelativPersonrolle.BARN,
                        it.relatertPersonUtenFolkeregisteridentifikator!!.tilRelatertPerson(),
                    )
                }

        val personer =
            barnFnr?.let { fnr ->
                pdlKlient.hentPersonBolk(fnr, saktyper).data?.hentPersonBolk?.map {
                    mapPerson(
                        Folkeregisteridentifikator.of(it.ident),
                        PersonRolle.TILKNYTTET_BARN,
                        it.person!!,
                        saktyper,
                    )
                }
            }

        return personer?.let { Barnekull(it, personerUtenIdent) }
    }

    private suspend fun mapBarnekullPersonopplysning(
        forelder: PdlHentPerson,
        sakType: SakType,
        brukerTokenInfo: BrukerTokenInfo,
    ): BarnekullPersonopplysning? {
        val barnFnr =
            forelder.forelderBarnRelasjon
                ?.filter {
                    it.relatertPersonsRolle == PdlForelderBarnRelasjonRolle.BARN &&
                        it.relatertPersonsIdent != null
                }?.map { it.relatertPersonsIdent }
                ?.distinct()
                ?.map { Folkeregisteridentifikator.of(it) }

        val personer =
            barnFnr?.let { fnr ->
                fnr.map { ident ->
                    pdlOboKlient
                        .hentPerson(
                            ident,
                            PersonRolle.TILKNYTTET_BARN,
                            bruker = brukerTokenInfo,
                            sakType = sakType,
                        ).data
                        ?.hentPerson
                        ?.let {
                            mapFamiliemedlem(
                                it,
                                ident,
                                sakType,
                                brukerTokenInfo = brukerTokenInfo,
                                PersonRolle.TILKNYTTET_BARN,
                            )
                        }
                }
            }

        return personer?.let { BarnekullPersonopplysning(it) }
    }

    private suspend fun kanskjeAvklarStatsborgerskap(
        statsborgerskap: List<PdlStatsborgerskap>?,
    ): Pair<PdlStatsborgerskap?, List<Statsborgerskap>?> {
        if (statsborgerskap == null) {
            return null to null
        }
        try {
            return ppsKlient.avklarStatsborgerskap(statsborgerskap) to null
        } catch (e: Exception) {
            logger.warn("PPS feilet i avklaring av statsborgerskap, sender tilbake fullt grunnlag fra PDL", e)
            return null to statsborgerskap.map { it.tilStatsborgerskap() }
        }
    }

    private suspend fun avklarSivilstatus(
        folkeregisteridentifikator: Folkeregisteridentifikator,
        pdlSivilstand: List<PdlSivilstand>,
    ): OpplysningDTO<Sivilstatus>? =
        try {
            ppsKlient
                .avklarSivilstand(pdlSivilstand, folkeregisteridentifikator)
                ?.let {
                    OpplysningDTO(
                        Sivilstatus.valueOf(it.type.name),
                        it.metadata.opplysningsId,
                    )
                }
        } catch (e: ParallelleSannheterException) {
            if (e.ppsStatus == HttpStatusCode.NotImplemented) {
                OpplysningDTO(
                    Sivilstatus.UAVKLART_PPS,
                    UUID.randomUUID().toString(),
                )
            } else {
                throw e
            }
        }

    private fun PdlStatsborgerskap.tilStatsborgerskap(): Statsborgerskap =
        Statsborgerskap(
            land = this.land,
            gyldigFraOgMed = this.gyldigFraOgMed,
            gyldigTilOgMed = this.gyldigTilOgMed,
        )
}

data class Barnekull(
    val barn: List<Person>,
    val barnUtenIdent: List<PersonUtenIdent>? = null,
)

data class BarnekullPersonopplysning(
    val barn: List<Familiemedlem?>,
)
