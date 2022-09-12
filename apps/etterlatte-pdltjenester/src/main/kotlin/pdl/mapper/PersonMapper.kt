package no.nav.etterlatte.pdl.mapper

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Adressebeskyttelse
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Sivilstatus
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlHentPerson
import no.nav.etterlatte.pdl.PdlKlient

object PersonMapper {

    fun mapPerson(
        ppsKlient: ParallelleSannheterKlient,
        pdlKlient: PdlKlient,
        fnr: Foedselsnummer,
        personRolle: PersonRolle,
        hentPerson: PdlHentPerson
    ): Person = runBlocking {
        val navn = ppsKlient.avklarNavn(hentPerson.navn)
        val adressebeskyttelse = ppsKlient.avklarAdressebeskyttelse(hentPerson.adressebeskyttelse)
        val statsborgerskap = hentPerson.statsborgerskap?.let { ppsKlient.avklarStatsborgerskap(it) }
        val sivilstand = hentPerson.sivilstand?.let { ppsKlient.avklarSivilstand(it) }
        val foedsel = ppsKlient.avklarFoedsel(hentPerson.foedsel)
        val doedsfall = ppsKlient.avklarDoedsfall(hentPerson.doedsfall)

        Person(
            fornavn = navn.fornavn,
            etternavn = navn.etternavn,
            foedselsnummer = fnr,
            foedselsdato = foedsel.foedselsdato,
            foedselsaar = foedsel.foedselsaar,
            doedsdato = doedsfall?.doedsdato,
            foedeland = foedsel.foedeland,
            adressebeskyttelse = adressebeskyttelse?.let { Adressebeskyttelse.valueOf(it.gradering.toString()) }
                ?: Adressebeskyttelse.UGRADERT,
            bostedsadresse = hentPerson.bostedsadresse?.let { AdresseMapper.mapBostedsadresse(ppsKlient, it) },
            oppholdsadresse = hentPerson.oppholdsadresse?.let { AdresseMapper.mapOppholdsadresse(ppsKlient, it) },
            deltBostedsadresse = hentPerson.deltBostedsadresse?.let {
                AdresseMapper.mapDeltBostedsadresse(ppsKlient, it)
            },
            kontaktadresse = hentPerson.kontaktadresse?.let { AdresseMapper.mapKontaktadresse(ppsKlient, it) },
            statsborgerskap = statsborgerskap?.land,
            sivilstatus = sivilstand?.let { Sivilstatus.valueOf(it.type.name) } ?: Sivilstatus.UOPPGITT,
            utland = UtlandMapper.mapUtland(hentPerson),
            familieRelasjon = FamilieRelasjonMapper.mapFamilieRelasjon(hentPerson, personRolle),
            avdoedesBarn = if (personRolle == PersonRolle.AVDOED) {
                BarnekullMapper.mapBarnekull(
                    pdlKlient,
                    ppsKlient,
                    hentPerson
                )
            } else {
                null
            },
            vergemaalEllerFremtidsfullmakt = hentPerson.vergemaalEllerFremtidsfullmakt?.let { VergeMapper.mapVerge(it) }
        )
    }

    fun mapOpplysningsperson(
        ppsKlient: ParallelleSannheterKlient,
        pdlKlient: PdlKlient,
        fnr: Foedselsnummer,
        personRolle: PersonRolle,
        hentPerson: PdlHentPerson
    ): PersonDTO = runBlocking {
        val navn = ppsKlient.avklarNavn(hentPerson.navn)
        val adressebeskyttelse = ppsKlient.avklarAdressebeskyttelse(hentPerson.adressebeskyttelse)
        val statsborgerskap = hentPerson.statsborgerskap?.let { ppsKlient.avklarStatsborgerskap(it) }
        val sivilstand = hentPerson.sivilstand?.let { ppsKlient.avklarSivilstand(it) }
        val foedsel = ppsKlient.avklarFoedsel(hentPerson.foedsel)
        val doedsfall = ppsKlient.avklarDoedsfall(hentPerson.doedsfall)

        PersonDTO(
            fornavn = OpplysningDTO(navn.fornavn, navn.metadata.opplysningsId),
            etternavn = OpplysningDTO(navn.etternavn, navn.metadata.opplysningsId),
            foedselsnummer = OpplysningDTO(fnr, null),
            foedselsdato = foedsel.foedselsdato?.let { OpplysningDTO(it, foedsel.metadata.opplysningsId) },
            foedselsaar = OpplysningDTO(foedsel.foedselsaar, foedsel.metadata.opplysningsId),
            doedsdato = doedsfall?.doedsdato?.let { OpplysningDTO(it, doedsfall.metadata.opplysningsId) },
            foedeland = foedsel.foedeland?.let { OpplysningDTO(it, foedsel.metadata.opplysningsId) },
            adressebeskyttelse = adressebeskyttelse?.let {
                OpplysningDTO(Adressebeskyttelse.valueOf(it.gradering.toString()), it.metadata.opplysningsId)
            } ?: OpplysningDTO(Adressebeskyttelse.UGRADERT, null),
            bostedsadresse = hentPerson.bostedsadresse?.let { AdresseMapper.mapBostedsadresse(ppsKlient, it) }
                ?.map { OpplysningDTO(it, null) }, /* Finn ut hva opplysningsid:n er for data fra pps */
            oppholdsadresse = hentPerson.oppholdsadresse?.let { AdresseMapper.mapOppholdsadresse(ppsKlient, it) }
                ?.map { OpplysningDTO(it, null) },
            deltBostedsadresse = hentPerson.deltBostedsadresse?.let {
                AdresseMapper.mapDeltBostedsadresse(ppsKlient, it)
            }?.map { OpplysningDTO(it, null) },
            kontaktadresse = hentPerson.kontaktadresse?.let { AdresseMapper.mapKontaktadresse(ppsKlient, it) }
                ?.map { OpplysningDTO(it, null) },
            statsborgerskap = statsborgerskap?.let { OpplysningDTO(it.land, it.metadata.opplysningsId) },
            sivilstatus = sivilstand?.let {
                OpplysningDTO(
                    Sivilstatus.valueOf(it.type.name),
                    it.metadata.opplysningsId
                )
            },
            utland = OpplysningDTO(UtlandMapper.mapUtland(hentPerson), null),
            familieRelasjon = OpplysningDTO(
                FamilieRelasjonMapper.mapFamilieRelasjon(hentPerson, personRolle),
                null
            ), // TODO ai: tre opplysninger i en
            avdoedesBarn = if (personRolle == PersonRolle.AVDOED) {
                BarnekullMapper.mapBarnekull(
                    pdlKlient,
                    ppsKlient,
                    hentPerson
                )
            } else {
                null
            },
            vergemaalEllerFremtidsfullmakt = hentPerson.vergemaalEllerFremtidsfullmakt?.let { VergeMapper.mapVerge(it) }
                ?.map { OpplysningDTO(it, null) }
        )
    }
}