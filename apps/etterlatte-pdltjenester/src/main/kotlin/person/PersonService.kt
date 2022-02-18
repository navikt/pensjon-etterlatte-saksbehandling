package no.nav.etterlatte.person

import io.ktor.features.NotFoundException
import no.nav.etterlatte.libs.common.pdl.EyHentUtvidetPersonRequest
import no.nav.etterlatte.libs.common.pdl.Gradering
import no.nav.etterlatte.libs.common.pdl.ResponseError
import no.nav.etterlatte.libs.common.pdl.Variables
import no.nav.etterlatte.libs.common.person.*
import no.nav.etterlatte.person.pdl.ForelderBarnRelasjonRolle

import no.nav.etterlatte.person.pdl.HentUtvidetPerson
import no.nav.etterlatte.person.pdl.InnflyttingTilNorge
import no.nav.etterlatte.person.pdl.UtflyttingFraNorge

import org.slf4j.LoggerFactory


//TODO vurdere å refaktorere til ulike serviceklasser
class PersonService(
    private val klient: PersonKlient
) {
    private val logger = LoggerFactory.getLogger(PersonService::class.java)
    private val adressebeskyttet = listOf(
        Gradering.FORTROLIG, Gradering.STRENGT_FORTROLIG,
        Gradering.STRENGT_FORTROLIG_UTLAND
    )


    suspend fun hentPerson(variables: EyHentUtvidetPersonRequest): Person {
        logger.info("Henter person fra PDL")

        val response = klient.hentPerson(
            Variables(
                ident = variables.foedselsnummer,
                historikk = variables.historikk,
                adresse = variables.adresse,
                utland = variables.utland,
                familieRelasjon = variables.familieRelasjon
            )
        )

        val hentPerson = response.data?.hentPerson

        //TODO fikse feilhåndtering
        if (hentPerson == null) {
            println("XXX Response: " + response.toString())
            logger.info("XXX Response: " + response.data?.toString())
            loggfoerFeilmeldinger(response.errors)
            throw NotFoundException()
        }

        return opprettUtvidetPerson(Foedselsnummer.of(variables.foedselsnummer), hentPerson)
    }

    private fun opprettUtvidetPerson(
        fnr: Foedselsnummer,
        hentPerson: HentUtvidetPerson
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
            //TODO rolle disse til noe fornuftig
            rolle = null,
            familieRelasjon = opprettFamilieRelasjon(hentPerson)

        )
    }

    private fun opprettAdresse(hentPerson: HentUtvidetPerson): eyAdresse {
        val bostedsadresse = hentPerson.bostedsadresse
            ?.maxByOrNull { it.metadata.sisteRegistrertDato() }
        val kontaktsadresse = hentPerson.kontaktadresse
            ?.maxByOrNull { it.metadata.sisteRegistrertDato() }
        val oppholdssadresse = hentPerson.oppholdsadresse
            ?.maxByOrNull { it.metadata.sisteRegistrertDato() }

        return eyAdresse(
            bostedsadresse = EyBostedsadresse(
                EyVegadresse(
                    adressenavn = bostedsadresse?.vegadresse?.adressenavn,
                    husnummer = bostedsadresse?.vegadresse?.husnummer,
                    husbokstav = bostedsadresse?.vegadresse?.husbokstav,
                    postnummer = bostedsadresse?.vegadresse?.postnummer,
                )
            ),
            kontaktadresse = EyKontaktadresse(
                EyVegadresse(
                    adressenavn = kontaktsadresse?.vegadresse?.adressenavn,
                    husnummer = kontaktsadresse?.vegadresse?.husnummer,
                    husbokstav = kontaktsadresse?.vegadresse?.husbokstav,
                    postnummer = kontaktsadresse?.vegadresse?.postnummer,
                )
            ),
            oppholdsadresse = EyOppholdsadresse(
                EyVegadresse(
                    adressenavn = oppholdssadresse?.vegadresse?.adressenavn,
                    husnummer = oppholdssadresse?.vegadresse?.husnummer,
                    husbokstav = oppholdssadresse?.vegadresse?.husbokstav,
                    postnummer = oppholdssadresse?.vegadresse?.postnummer,
                )
            ),
        )
    }




    private fun opprettUtland(hentPerson: HentUtvidetPerson): eyUtland {
        return eyUtland(
            utflyttingFraNorge = hentPerson?.utflyttingFraNorge?.map { (mapUtflytting(it)) },
            innflyttingTilNorge = hentPerson?.innflyttingTilNorge?.map { (mapInnflytting(it)) }
        )
    }

    private fun mapUtflytting(utflytting: UtflyttingFraNorge): eyUtflyttingFraNorge {
        return eyUtflyttingFraNorge(
            tilflyttingsland = utflytting.tilflyttingsland,
            dato = utflytting.utflyttingsdato.toString()
        )
    }

    private fun mapInnflytting(innflytting: InnflyttingTilNorge): eyInnflyttingTilNorge {
        return eyInnflyttingTilNorge(
            fraflyttingsland = innflytting.fraflyttingsland,
            //TODO her må vi heller sjekke mot gyldighetsdato på bostedsadresse
            //TODO skal ikke være tostring her
            dato = innflytting.folkeregistermetadata?.gyldighetstidspunkt.toString()
        )
    }
    private fun opprettFamilieRelasjon(hentPerson: HentUtvidetPerson): EyFamilieRelasjon {


        //TODO tar kun med foreldreAnsvar med fnr nå
        return EyFamilieRelasjon(
            ansvarligeForeldre = hentPerson?.foreldreansvar?.filter { it.ansvarlig != null }?.map {

                EyForeldreAnsvar(
                    Foedselsnummer.of(
                        it.ansvarlig
                    )
                )

            },
            foreldre =
            hentPerson?.forelderBarnRelasjon?.filter { it.minRolleForPerson != ForelderBarnRelasjonRolle.BARN }
                ?.map {
                    EyForeldre(
                        Foedselsnummer.of(it.relatertPersonsIdent)
                    )
                },
            barn =
            hentPerson?.forelderBarnRelasjon?.filter { it.minRolleForPerson == ForelderBarnRelasjonRolle.BARN }
                ?.map {
                    EyBarn(
                        Foedselsnummer.of(it.relatertPersonsIdent)
                    )
                }
        )
    }

    private fun loggfoerFeilmeldinger(errors: List<ResponseError>?) {
        logger.error("Kunne ikke hente person fra PDL")

        errors?.forEach {
            logger.error(it.message)
            println(it.message)
        }
    }
}
