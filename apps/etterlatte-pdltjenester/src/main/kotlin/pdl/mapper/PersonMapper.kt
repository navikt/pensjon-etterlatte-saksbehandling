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
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlHentPerson
import no.nav.etterlatte.pdl.PdlKlient
import no.nav.etterlatte.pdl.PdlStatsborgerskap
import org.slf4j.LoggerFactory

object PersonMapper {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun mapPerson(
        ppsKlient: ParallelleSannheterKlient,
        pdlKlient: PdlKlient,
        fnr: Folkeregisteridentifikator,
        personRolle: PersonRolle,
        hentPerson: PdlHentPerson,
        saktype: SakType,
    ): Person =
        runBlocking {
            val navn = ppsKlient.avklarNavn(hentPerson.navn)
            val adressebeskyttelse = ppsKlient.avklarAdressebeskyttelse(hentPerson.adressebeskyttelse)
            val (statsborgerskap, pdlStatsborgerskap) =
                kanskjeAvklarStatsborgerskap(
                    ppsKlient,
                    hentPerson.statsborgerskap,
                )
            val ppsSivilstand = hentPerson.sivilstand?.let { ppsKlient.avklarSivilstand(it) }
            val foedsel = ppsKlient.avklarFoedsel(hentPerson.foedsel)
            val doedsfall = ppsKlient.avklarDoedsfall(hentPerson.doedsfall)
            val barnekull =
                if (personRolle == PersonRolle.AVDOED) {
                    BarnekullMapper.mapBarnekull(
                        pdlKlient,
                        ppsKlient,
                        hentPerson,
                        saktype,
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
            val ppsSivilstand = hentPerson.sivilstand?.let { ppsKlient.avklarSivilstand(it) }
            val foedsel = ppsKlient.avklarFoedsel(hentPerson.foedsel)
            val doedsfall = ppsKlient.avklarDoedsfall(hentPerson.doedsfall)
            val barnekull =
                if (request.rolle == PersonRolle.AVDOED) {
                    BarnekullMapper.mapBarnekull(
                        pdlKlient,
                        ppsKlient,
                        hentPerson,
                        request.saktype,
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
                bostedsadresse =
                    hentPerson.bostedsadresse?.let { AdresseMapper.mapBostedsadresse(ppsKlient, it) }
                        ?.map { OpplysningDTO(it, null) }, // Finn ut hva opplysningsid:n er for data fra pps
                oppholdsadresse =
                    hentPerson.oppholdsadresse?.let { AdresseMapper.mapOppholdsadresse(ppsKlient, it) }
                        ?.map { OpplysningDTO(it, null) },
                deltBostedsadresse =
                    hentPerson.deltBostedsadresse?.let {
                        AdresseMapper.mapDeltBostedsadresse(ppsKlient, it)
                    }?.map { OpplysningDTO(it, null) },
                kontaktadresse =
                    hentPerson.kontaktadresse?.let { AdresseMapper.mapKontaktadresse(ppsKlient, it) }
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
                    hentPerson.sivilstand?.let { SivilstandMapper.mapSivilstand(it) }
                        ?.map { OpplysningDTO(it, null) },
                utland = OpplysningDTO(UtlandMapper.mapUtland(hentPerson), null),
                familieRelasjon =
                    OpplysningDTO(
                        FamilieRelasjonMapper.mapFamilieRelasjon(hentPerson, request.rolle),
                        null,
                    ), // TODO ai: tre opplysninger i en
                avdoedesBarn = barnekull?.barn,
                vergemaalEllerFremtidsfullmakt =
                    hentPerson.vergemaalEllerFremtidsfullmakt?.let { VergeMapper.mapVerge(it) }
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

fun PdlStatsborgerskap.tilStatsborgerskap(): Statsborgerskap {
    return Statsborgerskap(
        land = this.land,
        gyldigFraOgMed = this.gyldigFraOgMed,
        gyldigTilOgMed = this.gyldigTilOgMed,
    )
}
