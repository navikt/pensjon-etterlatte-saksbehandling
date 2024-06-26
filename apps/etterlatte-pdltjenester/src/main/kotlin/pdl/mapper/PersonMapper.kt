package no.nav.etterlatte.pdl.mapper

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.libs.common.person.Statsborgerskap
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlHentPerson
import no.nav.etterlatte.pdl.PdlHentPersonNavnFoedselsdato
import no.nav.etterlatte.pdl.PdlKlient
import no.nav.etterlatte.pdl.PdlOboKlient
import no.nav.etterlatte.pdl.PdlStatsborgerskap
import no.nav.etterlatte.pdl.SoekPersonTreff
import no.nav.etterlatte.personweb.dto.PersonNavnFoedselsaar
import no.nav.etterlatte.personweb.dto.PersonSoekSvar
import no.nav.etterlatte.personweb.familieOpplysninger.Bostedsadresse
import no.nav.etterlatte.personweb.familieOpplysninger.Familiemedlem
import no.nav.etterlatte.personweb.familieOpplysninger.Familierelasjon
import no.nav.etterlatte.personweb.familieOpplysninger.Sivilstand
import org.slf4j.LoggerFactory

object PersonMapper {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun mapPerson(
        ppsKlient: ParallelleSannheterKlient,
        pdlKlient: PdlKlient,
        fnr: Folkeregisteridentifikator,
        personRolle: PersonRolle,
        hentPerson: PdlHentPerson,
        saktyper: List<SakType>,
    ): Person =
        runBlocking {
            val navn = ppsKlient.avklarNavn(hentPerson.navn)
            val adressebeskyttelse = ppsKlient.avklarAdressebeskyttelse(hentPerson.adressebeskyttelse)
            val (statsborgerskap, pdlStatsborgerskap) =
                kanskjeAvklarStatsborgerskap(
                    ppsKlient,
                    hentPerson.statsborgerskap,
                )
            val ppsSivilstand = hentPerson.sivilstand?.let { ppsKlient.avklarSivilstand(it, fnr) }
            val foedsel = ppsKlient.avklarFoedsel(hentPerson.foedsel)
            val doedsfall = ppsKlient.avklarDoedsfall(hentPerson.doedsfall)
            val barnekull =
                if (personRolle == PersonRolle.AVDOED) {
                    BarnekullMapper.mapBarnekull(
                        pdlKlient,
                        ppsKlient,
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
                foedselsdato = foedsel.foedselsdato,
                foedselsaar = foedsel.foedselsaar,
                doedsdato = doedsfall?.doedsdato,
                foedeland = foedsel.foedeland,
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
                sivilstatus = ppsSivilstand?.let { Sivilstatus.valueOf(it.type.name) } ?: Sivilstatus.UOPPGITT,
                sivilstand = hentPerson.sivilstand?.let { SivilstandMapper.mapSivilstand(it) },
                utland = UtlandMapper.mapUtland(hentPerson),
                familieRelasjon =
                    FamilieRelasjonMapper.mapFamilieRelasjon(
                        hentPerson,
                        personRolle,
                    ),
                avdoedesBarn = barnekull?.barn,
                avdoedesBarnUtenIdent = barnekull?.barnUtenIdent,
                vergemaalEllerFremtidsfullmakt =
                    hentPerson.vergemaalEllerFremtidsfullmakt?.let {
                        VergeMapper.mapVerge(
                            it,
                        )
                    },
            )
        }

    fun mapFamiliemedlem(
        ppsKlient: ParallelleSannheterKlient,
        pdlOboKlient: PdlOboKlient,
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
                    ppsKlient,
                    hentPerson.statsborgerskap,
                )
            val sivilstand = hentPerson.sivilstand?.let { SivilstandMapper.mapSivilstand(it) }
            val foedsel = ppsKlient.avklarFoedsel(hentPerson.foedsel)
            val doedsfall = ppsKlient.avklarDoedsfall(hentPerson.doedsfall)
            val bostedsadresse = hentPerson.bostedsadresse?.let { AdresseMapper.mapBostedsadresse(ppsKlient, it) }
            val barnekull =
                BarnekullMapper.mapBarnekullPersonopplysning(
                    ppsKlient,
                    pdlOboKlient,
                    brukerTokenInfo = brukerTokenInfo,
                    forelder = hentPerson,
                    sakType = sakType,
                )

            Familiemedlem(
                fornavn = navn.fornavn,
                etternavn = navn.etternavn,
                foedselsnummer = ident,
                foedselsdato = foedsel.foedselsdato,
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
                vergemaalEllerFremtidsfullmakt =
                    hentPerson.vergemaalEllerFremtidsfullmakt?.let {
                        VergeMapper.mapVerge(
                            it,
                        )
                    },
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

    fun mapPersonNavnFoedsel(
        ppsKlient: ParallelleSannheterKlient,
        ident: String,
        hentPerson: PdlHentPersonNavnFoedselsdato,
    ): PersonNavnFoedselsaar =
        runBlocking {
            val navn = ppsKlient.avklarNavn(hentPerson.navn)

            val fnr =
                if (Folkeregisteridentifikator.isValid(ident)) {
                    ident
                } else {
                    ppsKlient
                        .avklarFolkeregisteridentifikator(hentPerson.folkeregisteridentifikator)
                        .identifikasjonsnummer
                }
            val foedsel = ppsKlient.avklarFoedsel(hentPerson.foedsel)
            val doedsfall = ppsKlient.avklarDoedsfall(hentPerson.doedsfall)

            PersonNavnFoedselsaar(
                fornavn = navn.fornavn,
                mellomnavn = navn.mellomnavn,
                etternavn = navn.etternavn,
                foedselsnummer = Folkeregisteridentifikator.of(fnr),
                foedselsdato = foedsel.foedselsdato,
                foedselsaar = foedsel.foedselsaar,
                doedsdato = doedsfall?.doedsdato,
            )
        }

    suspend fun mapPersonSoek(
        ppsKlient: ParallelleSannheterKlient,
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
        ppsKlient: ParallelleSannheterKlient,
        pdlKlient: PdlKlient,
        request: HentPersonRequest,
        hentPerson: PdlHentPerson,
    ): PersonDTO =
        runBlocking {
            val navn = ppsKlient.avklarNavn(hentPerson.navn)
            val adressebeskyttelse = ppsKlient.avklarAdressebeskyttelse(hentPerson.adressebeskyttelse)
            val (statsborgerskap, statsborgerskapPdl) =
                kanskjeAvklarStatsborgerskap(
                    ppsKlient,
                    hentPerson.statsborgerskap,
                )
            val ppsSivilstand = hentPerson.sivilstand?.let { ppsKlient.avklarSivilstand(it, request.foedselsnummer) }
            val foedsel = ppsKlient.avklarFoedsel(hentPerson.foedsel)
            val doedsfall = ppsKlient.avklarDoedsfall(hentPerson.doedsfall)
            val barnekull =
                if (request.rolle == PersonRolle.AVDOED) {
                    BarnekullMapper.mapBarnekull(
                        pdlKlient,
                        ppsKlient,
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
                foedselsnummer = OpplysningDTO(request.foedselsnummer, null),
                foedselsdato = foedsel.foedselsdato?.let { OpplysningDTO(it, foedsel.metadata.opplysningsId) },
                foedselsaar = OpplysningDTO(foedsel.foedselsaar, foedsel.metadata.opplysningsId),
                doedsdato = doedsfall?.doedsdato?.let { OpplysningDTO(it, doedsfall.metadata.opplysningsId) },
                foedeland = foedsel.foedeland?.let { OpplysningDTO(it, foedsel.metadata.opplysningsId) },
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
                sivilstatus =
                    ppsSivilstand?.let {
                        OpplysningDTO(
                            Sivilstatus.valueOf(it.type.name),
                            it.metadata.opplysningsId,
                        )
                    },
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
                    hentPerson.vergemaalEllerFremtidsfullmakt
                        ?.let { VergeMapper.mapVerge(it) }
                        ?.map { OpplysningDTO(it, null) },
            )
        }

    private suspend fun kanskjeAvklarStatsborgerskap(
        ppsKlient: ParallelleSannheterKlient,
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
}

fun PdlStatsborgerskap.tilStatsborgerskap(): Statsborgerskap =
    Statsborgerskap(
        land = this.land,
        gyldigFraOgMed = this.gyldigFraOgMed,
        gyldigTilOgMed = this.gyldigTilOgMed,
    )
