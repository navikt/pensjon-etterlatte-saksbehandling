package no.nav.etterlatte.person

import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.Barn
import no.nav.etterlatte.libs.common.person.Bostedsadresse
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Foreldre
import no.nav.etterlatte.libs.common.person.ForeldreAnsvar
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.InnflyttingTilNorge
import no.nav.etterlatte.libs.common.person.Kontaktadresse
import no.nav.etterlatte.libs.common.person.Oppholdsadresse
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.UtflyttingFraNorge
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.person.Vegadresse
import no.nav.etterlatte.person.pdl.ForelderBarnRelasjonRolle
import no.nav.etterlatte.person.pdl.Gradering
import no.nav.etterlatte.person.pdl.HentPerson
import no.nav.etterlatte.person.pdl.PdlInnflyttingTilNorge
import no.nav.etterlatte.person.pdl.PdlUtflyttingFraNorge
import no.nav.etterlatte.person.pdl.PdlVariables
import org.slf4j.LoggerFactory

class PdlForesporselFeilet(message: String) : RuntimeException(message)

class PersonService(
    private val pdlKlient: PdlKlient
) {
    private val logger = LoggerFactory.getLogger(PersonService::class.java)
    private val adressebeskyttet = listOf(
        Gradering.FORTROLIG, Gradering.STRENGT_FORTROLIG,
        Gradering.STRENGT_FORTROLIG_UTLAND
    )

    suspend fun hentPerson(hentPersonRequest: HentPersonRequest): Person {
        logger.info("Henter person med fnr=${hentPersonRequest.foedselsnummer} fra PDL")

        return pdlKlient.hentPerson(hentPersonRequest.toPdlVariables()).let {
            if (it.data?.hentPerson == null) {
                val pdlFeil = it.errors?.joinToString(", ")
                throw PdlForesporselFeilet(
                    "Kunne ikke hente person med fnr=${hentPersonRequest.foedselsnummer} fra PDL: $pdlFeil"
                )
            } else {
                opprettPerson(hentPersonRequest.foedselsnummer, it.data.hentPerson)
            }
        }
    }

    private fun HentPersonRequest.toPdlVariables() = PdlVariables(
        ident = foedselsnummer.value,
        historikk = historikk,
        adresse = adresse,
        utland = utland,
        familieRelasjon = familieRelasjon
    )

    private fun opprettPerson(
        fnr: Foedselsnummer,
        hentPerson: HentPerson
    ): Person {
        val navn = hentPerson.navn
            .maxByOrNull { it.metadata.sisteRegistrertDato() }!!

        val adressebeskyttelse = hentPerson.adressebeskyttelse
            .any { it.gradering in adressebeskyttet }

        val statsborgerskap = hentPerson.statsborgerskap
            ?.maxByOrNull { it.metadata.sisteRegistrertDato() }

        val sivilstand = hentPerson.sivilstand
            .maxByOrNull { it.metadata.sisteRegistrertDato() }

        val foedsel = hentPerson.foedsel
            .maxByOrNull { it.metadata.sisteRegistrertDato() }

        val doedsfall = hentPerson.doedsfall
            .maxByOrNull { it.metadata.sisteRegistrertDato() }

        return Person(
            fornavn = navn.fornavn,
            etternavn = navn.etternavn,
            foedselsnummer = fnr,
            foedselsdato = foedsel?.foedselsdato?.toString(),
            foedselsaar = foedsel?.foedselsaar,
            doedsdato = doedsfall?.doedsdato.toString(),
            adressebeskyttelse = adressebeskyttelse,
            adresse = opprettAdresse(hentPerson),
            statsborgerskap = statsborgerskap?.land,
            foedeland = foedsel?.foedeland,
            sivilstatus = sivilstand?.type?.name,

            utland = opprettUtland(hentPerson),
            //TODO hva gjør vi med rolle?
            rolle = null,
            familieRelasjon = opprettFamilieRelasjon(hentPerson)
        )
    }

    private fun opprettAdresse(hentPerson: HentPerson): Adresse {
        val bostedsadresse = hentPerson.bostedsadresse
            ?.maxByOrNull { it.metadata.sisteRegistrertDato() }
        val kontaktsadresse = hentPerson.kontaktadresse
            ?.maxByOrNull { it.metadata.sisteRegistrertDato() }
        val oppholdssadresse = hentPerson.oppholdsadresse
            ?.maxByOrNull { it.metadata.sisteRegistrertDato() }

        return Adresse(
            bostedsadresse = Bostedsadresse(
                Vegadresse(
                    adressenavn = bostedsadresse?.vegadresse?.adressenavn,
                    husnummer = bostedsadresse?.vegadresse?.husnummer,
                    husbokstav = bostedsadresse?.vegadresse?.husbokstav,
                    postnummer = bostedsadresse?.vegadresse?.postnummer,
                )
            ),
            kontaktadresse = Kontaktadresse(
                Vegadresse(
                    adressenavn = kontaktsadresse?.vegadresse?.adressenavn,
                    husnummer = kontaktsadresse?.vegadresse?.husnummer,
                    husbokstav = kontaktsadresse?.vegadresse?.husbokstav,
                    postnummer = kontaktsadresse?.vegadresse?.postnummer,
                )
            ),
            oppholdsadresse = Oppholdsadresse(
                Vegadresse(
                    adressenavn = oppholdssadresse?.vegadresse?.adressenavn,
                    husnummer = oppholdssadresse?.vegadresse?.husnummer,
                    husbokstav = oppholdssadresse?.vegadresse?.husbokstav,
                    postnummer = oppholdssadresse?.vegadresse?.postnummer,
                )
            ),
        )
    }

    private fun opprettUtland(hentPerson: HentPerson): Utland {
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

    private fun opprettFamilieRelasjon(hentPerson: HentPerson): FamilieRelasjon {
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
                ?.filter { it.relatertPersonsRolle != ForelderBarnRelasjonRolle.BARN }
                ?.groupBy { it.relatertPersonsIdent }
                ?.mapValues { it.value.maxByOrNull { fbr -> fbr.metadata.sisteRegistrertDato() } }
                ?.map {
                    Foreldre(Foedselsnummer.of(it.value?.relatertPersonsIdent))
                },

            barn = hentPerson.forelderBarnRelasjon
                ?.filter { it.relatertPersonsRolle == ForelderBarnRelasjonRolle.BARN }
                ?.groupBy { it.relatertPersonsIdent }
                ?.mapValues { it.value.maxByOrNull { fbr -> fbr.metadata.sisteRegistrertDato() } }
                ?.map {
                    Barn(Foedselsnummer.of(it.value?.relatertPersonsIdent))
                }
        )
    }

}
