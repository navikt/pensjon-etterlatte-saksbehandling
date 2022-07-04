package no.nav.etterlatte.pdl.mapper

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.person.*
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlForelderBarnRelasjonRolle
import no.nav.etterlatte.pdl.PdlHentPerson
import no.nav.etterlatte.pdl.PdlKlient

object BarnekullMapper {

    suspend fun mapBarnekull(pdlKlient: PdlKlient, ppsKlient: ParallelleSannheterKlient,forelder: PdlHentPerson): List<Person>? {
        val barnFnr = forelder.forelderBarnRelasjon
            ?.filter { it.relatertPersonsRolle == PdlForelderBarnRelasjonRolle.BARN }
            ?.groupBy { it.relatertPersonsIdent }
            ?.mapValues { it.value.maxByOrNull { fbr -> fbr.metadata.sisteRegistrertDato() } }
            ?.map {
                (Foedselsnummer.of(it.value?.relatertPersonsIdent))
            }
        println("barnFnr: $barnFnr")
        return if (barnFnr != null) {
            val bah = pdlKlient.hentPersonBolk(barnFnr, PersonRolle.BARN)
            println(bah)
            return bah.data?.hentPersonBolk?.map { mapBarn(ppsKlient,it.ident,it.person!!)}
        } else null
    }
    private fun mapBarn(
        ppsKlient: ParallelleSannheterKlient,
        fnr: String,
        hentPerson: PdlHentPerson
    ): Person = runBlocking {

        val navn = ppsKlient.avklarNavn(hentPerson.navn)
        val adressebeskyttelse = ppsKlient.avklarAdressebeskyttelse(hentPerson.adressebeskyttelse)
        val statsborgerskap = hentPerson.statsborgerskap?.let { ppsKlient.avklarStatsborgerskap(it) }
        val sivilstand = hentPerson.sivilstand?.let { ppsKlient.avklarSivilstand(it) }
        val foedsel = ppsKlient.avklarFoedsel(hentPerson.foedsel)
        val doedsfall = ppsKlient.avklarDoedsfall(hentPerson.doedsfall)
        println("Forsøker å mappe et barn")
        Person(
            fornavn = navn.fornavn,
            etternavn = navn.etternavn,
            foedselsnummer = Foedselsnummer.of(fnr),
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
            familieRelasjon = FamilieRelasjonMapper.mapFamilieRelasjon(hentPerson, PersonRolle.BARN),
            avdoedesBarn = null,
            vergemaalEllerFremtidsfullmakt = hentPerson.vergemaalEllerFremtidsfullmakt?.let{ VergeMapper.mapVerge(it)}
        )
    }

}