package no.nav.etterlatte.pdl.mapper

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.Adressebeskyttelse
import no.nav.etterlatte.libs.common.person.Barn
import no.nav.etterlatte.libs.common.person.Bostedsadresse
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Foreldre
import no.nav.etterlatte.libs.common.person.ForeldreAnsvar
import no.nav.etterlatte.libs.common.person.InnflyttingTilNorge
import no.nav.etterlatte.libs.common.person.Kontaktadresse
import no.nav.etterlatte.libs.common.person.Oppholdsadresse
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.UtflyttingFraNorge
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.person.Vegadresse
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.pdl.PdlForelderBarnRelasjonRolle
import no.nav.etterlatte.pdl.PdlHentPerson
import no.nav.etterlatte.pdl.PdlInnflyttingTilNorge
import no.nav.etterlatte.pdl.PdlUtflyttingFraNorge

object PersonMapper {

    fun mapPerson(
        ppsKlient: ParallelleSannheterKlient,
        fnr: Foedselsnummer,
        hentPerson: PdlHentPerson
    ): Person = runBlocking {
        val navn = ppsKlient.avklarNavn(hentPerson.navn)
        val adressebeskyttelse = ppsKlient.avklarAdressebeskyttelse(hentPerson.adressebeskyttelse)
        val statsborgerskap = hentPerson.statsborgerskap?.let { ppsKlient.avklarStatsborgerskap(it) }
        val sivilstand = ppsKlient.avklarSivilstand(hentPerson.sivilstand)
        val foedsel = ppsKlient.avklarFoedsel(hentPerson.foedsel)
        val doedsfall = ppsKlient.avklarDoedsfall(hentPerson.doedsfall)

        Person(
            fornavn = navn.fornavn,
            etternavn = navn.etternavn,
            foedselsnummer = fnr,
            foedselsdato = foedsel?.foedselsdato?.toString(),
            foedselsaar = foedsel?.foedselsaar,
            doedsdato = doedsfall?.doedsdato.toString(),
            adressebeskyttelse = adressebeskyttelse?.let {
                Adressebeskyttelse.valueOf(it.gradering.toString())
            } ?: Adressebeskyttelse.UGRADERT,
            adresse = opprettAdresse(ppsKlient, hentPerson),
            bostedsadresse = hentPerson.bostedsadresse?.let { AdresseMapper.mapBostedsadresse(ppsKlient, it) },
            oppholdsadresse = hentPerson.oppholdsadresse?.let { AdresseMapper.mapOppholdsadresse(ppsKlient, it) },
            statsborgerskap = statsborgerskap?.land,
            foedeland = foedsel?.foedeland,
            sivilstatus = sivilstand?.type?.name,
            utland = opprettUtland(hentPerson),
            familieRelasjon = opprettFamilieRelasjon(hentPerson)
        )
    }

    private fun opprettAdresse(
        ppsKlient: ParallelleSannheterKlient,
        hentPerson: PdlHentPerson
    ): Adresse = runBlocking {
        val bostedsadresse =hentPerson.bostedsadresse?.let { ppsKlient.avklarBostedsadresse(it) }
        val kontaktsadresse = hentPerson.kontaktadresse?.let { ppsKlient.avklarKontaktadresse(it) }
        val oppholdssadresse = hentPerson.oppholdsadresse?.let { ppsKlient.avklarOppholdsadresse(it) }

        Adresse(
            bostedsadresse = bostedsadresse?.let { Bostedsadresse(
                Vegadresse(
                    adressenavn = it.vegadresse?.adressenavn,
                    husnummer = it.vegadresse?.husnummer,
                    husbokstav = it.vegadresse?.husbokstav,
                    postnummer = it.vegadresse?.postnummer,
                )
            )
            },
            kontaktadresse = kontaktsadresse?.let { Kontaktadresse(
                Vegadresse(
                    adressenavn = it.vegadresse?.adressenavn,
                    husnummer = it.vegadresse?.husnummer,
                    husbokstav = it.vegadresse?.husbokstav,
                    postnummer = it.vegadresse?.postnummer,
                )
            )
            },
            oppholdsadresse = oppholdssadresse?.let { Oppholdsadresse(
                Vegadresse(
                    adressenavn = it.vegadresse?.adressenavn,
                    husnummer = it.vegadresse?.husnummer,
                    husbokstav = it.vegadresse?.husbokstav,
                    postnummer = it.vegadresse?.postnummer,
                )
            )
            },
        )
    }

    private fun opprettUtland(hentPerson: PdlHentPerson): Utland {
        return Utland(
            utflyttingFraNorge = hentPerson.utflyttingFraNorge?.map { (mapUtflytting(it)) },
            innflyttingTilNorge = hentPerson.innflyttingTilNorge?.map { (mapInnflytting(it)) }
        )
    }

    private fun mapUtflytting(utflytting: PdlUtflyttingFraNorge): UtflyttingFraNorge {
        return UtflyttingFraNorge(
            tilflyttingsland = utflytting.tilflyttingsland,
            dato = utflytting.utflyttingsdato.toString()
        )
    }

    private fun mapInnflytting(innflytting: PdlInnflyttingTilNorge): InnflyttingTilNorge {
        return InnflyttingTilNorge(
            fraflyttingsland = innflytting.fraflyttingsland,
            //TODO her må vi heller sjekke mot gyldighetsdato på bostedsadresse
            //TODO skal ikke være tostring her
            dato = innflytting.folkeregistermetadata?.gyldighetstidspunkt.toString()
        )
    }

    private fun opprettFamilieRelasjon(hentPerson: PdlHentPerson): FamilieRelasjon {
        //TODO tar kun med foreldreAnsvar med fnr nå
        //TODO finn ut om det er riktig å hente ut basert på sisteRegistrertDato
        return FamilieRelasjon(
            ansvarligeForeldre = hentPerson.foreldreansvar
                ?.filter { it.ansvarlig != null }
                ?.groupBy { it.ansvarlig }
                ?.mapValues { it.value.maxByOrNull { fa -> fa.metadata.sisteRegistrertDato() } }
                ?.map {
                    ForeldreAnsvar(Foedselsnummer.of(it.value?.ansvarlig))
                },

            foreldre = hentPerson.forelderBarnRelasjon
                ?.filter { it.relatertPersonsRolle != PdlForelderBarnRelasjonRolle.BARN }
                ?.groupBy { it.relatertPersonsIdent }
                ?.mapValues { it.value.maxByOrNull { fbr -> fbr.metadata.sisteRegistrertDato() } }
                ?.map {
                    Foreldre(Foedselsnummer.of(it.value?.relatertPersonsIdent))
                },

            barn = hentPerson.forelderBarnRelasjon
                ?.filter { it.relatertPersonsRolle == PdlForelderBarnRelasjonRolle.BARN }
                ?.groupBy { it.relatertPersonsIdent }
                ?.mapValues { it.value.maxByOrNull { fbr -> fbr.metadata.sisteRegistrertDato() } }
                ?.map {
                    Barn(Foedselsnummer.of(it.value?.relatertPersonsIdent))
                }
        )
    }

}