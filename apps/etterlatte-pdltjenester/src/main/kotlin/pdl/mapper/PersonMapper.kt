package no.nav.etterlatte.pdl.mapper

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.person.*
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlHentPerson

object PersonMapper {

    fun mapPerson(
        ppsKlient: ParallelleSannheterKlient,
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
            deltBostedsadresse = hentPerson.deltBostedsadresse?.let { AdresseMapper.mapDeltBostedsadresse(ppsKlient, it) },
            kontaktadresse = hentPerson.kontaktadresse?.let { AdresseMapper.mapKontaktadresse(ppsKlient, it) },
            statsborgerskap = statsborgerskap?.land,
            sivilstatus = sivilstand?.let { Sivilstatus.valueOf(it.type.name) } ?: Sivilstatus.UOPPGITT,
            utland = UtlandMapper.mapUtland(hentPerson),
            familieRelasjon = FamilieRelasjonMapper.mapFamilieRelasjon(hentPerson, personRolle),
        )
    }

}