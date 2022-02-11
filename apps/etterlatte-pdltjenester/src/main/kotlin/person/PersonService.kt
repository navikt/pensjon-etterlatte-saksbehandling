package no.nav.etterlatte.person

import io.ktor.features.NotFoundException
import no.nav.etterlatte.libs.common.pdl.EyHentAdresseRequest
import no.nav.etterlatte.libs.common.pdl.EyHentFamilieRelasjonRequest
import no.nav.etterlatte.libs.common.pdl.EyHentUtvidetPersonRequest
import no.nav.etterlatte.libs.common.pdl.Gradering
import no.nav.etterlatte.libs.common.pdl.ResponseError
import no.nav.etterlatte.libs.common.pdl.Variables
import no.nav.etterlatte.libs.common.person.EyBarn
import no.nav.etterlatte.libs.common.person.EyBostedsadresse
import no.nav.etterlatte.libs.common.person.EyFamilieRelasjon
import no.nav.etterlatte.libs.common.person.EyForeldre
import no.nav.etterlatte.libs.common.person.EyForeldreAnsvar
import no.nav.etterlatte.libs.common.person.EyKontaktadresse
import no.nav.etterlatte.libs.common.person.EyOppholdsadresse
import no.nav.etterlatte.libs.common.person.EyVegadresse
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.eyAdresse
import no.nav.etterlatte.libs.common.person.eyInnflyttingTilNorge
import no.nav.etterlatte.libs.common.person.eyUtflyttingFraNorge
import no.nav.etterlatte.libs.common.person.eyUtland
import no.nav.etterlatte.person.pdl.FamilieRelasjonResponse
import no.nav.etterlatte.person.pdl.ForelderBarnRelasjonRolle
import no.nav.etterlatte.person.pdl.HentPerson
import no.nav.etterlatte.person.pdl.utvidetperson.HentUtvidetPerson
import org.slf4j.LoggerFactory
import person.pdl.InnflyttingTilNorge
import person.pdl.UtflyttingFraNorge
import person.pdl.UtlandResponse
import person.pdl.adresse.AdresseResponse

//TODO vurdere å refaktorere til ulike serviceklasser
class PersonService(
    private val klient: PersonKlient
) {
    private val logger = LoggerFactory.getLogger(PersonService::class.java)
    private val adressebeskyttet = listOf(
        Gradering.FORTROLIG, Gradering.STRENGT_FORTROLIG,
        Gradering.STRENGT_FORTROLIG_UTLAND
    )

    suspend fun hentPerson(fnr: Foedselsnummer): Person {
        logger.info("Henter person fra PDL")

        val response = klient.hentPerson(fnr)

        val hentPerson = response.data?.hentPerson

        //TODO fikse feilhåndtering
        if (hentPerson == null) {
            println("XXX Response: " + response.toString())
            logger.info("XXX Response: " + response.data?.toString())
            loggfoerFeilmeldinger(response.errors)
            throw NotFoundException()
        }

        return opprettPerson(fnr, hentPerson)
    }
    suspend fun hentUtvidetPerson(variables: EyHentUtvidetPersonRequest): Person {
        logger.info("Henter person fra PDL")

        val response = klient.hentUtvidetPerson(
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

    suspend fun hentUtland(fnr: Foedselsnummer): eyUtland {
        logger.info("Henter utland fra PDL")

        val response = klient.hentUtland(fnr)
        println(response.toString())
        val hentUtland: UtlandResponse = response

        //TODO fikse feilhåndtering
        if (!response.errors.isNullOrEmpty()) {
            loggfoerFeilmeldinger(response.errors)
            throw NotFoundException()
        }

        return opprettUtland(hentUtland)
    }

    suspend fun hentAdresse(adresseRequest: EyHentAdresseRequest): eyAdresse {
        logger.info("Henter adresse fra PDL")

        val response = klient.hentAdresse(adresseRequest.fnr, adresseRequest.historikk)
        println(response.toString())
        val hentAdresse: AdresseResponse = response

        //TODO fikse feilhåndtering
        if (!response.errors.isNullOrEmpty()) {
            loggfoerFeilmeldinger(response.errors)
            throw NotFoundException()
        }

        return opprettAdresse(hentAdresse)
    }

    suspend fun hentFamilieRelasjon(familieRelasjonRequest: EyHentFamilieRelasjonRequest): EyFamilieRelasjon {
        logger.info("Henter adresse fra PDL")

        val response = klient.hentFamilieRelasjon(familieRelasjonRequest.fnr, familieRelasjonRequest.historikk)
        println(response.toString())
        val hentFamileRelasjon: FamilieRelasjonResponse = response

        //TODO fikse feilhåndtering
        if (!response.errors.isNullOrEmpty()) {
            loggfoerFeilmeldinger(response.errors)
            throw NotFoundException()
        }

        return opprettFamilieRelasjon(hentFamileRelasjon)
    }

    private fun opprettFamilieRelasjon(familieRelasjon: FamilieRelasjonResponse): EyFamilieRelasjon {

        return EyFamilieRelasjon(
            ansvarligeForeldre = familieRelasjon.data?.hentPerson?.foreldreansvar?.map {
                EyForeldreAnsvar(
                    Foedselsnummer.of(
                        it.ansvarlig
                    )
                )
            },
            foreldre = familieRelasjon.data?.hentPerson?.forelderBarnRelasjon?.filter { it.minRolleForPerson != ForelderBarnRelasjonRolle.BARN }
                ?.map {
                    EyForeldre(
                        Foedselsnummer.of(it.relatertPersonsIdent)
                    )
                },
            barn = familieRelasjon.data?.hentPerson?.forelderBarnRelasjon?.filter { it.minRolleForPerson == ForelderBarnRelasjonRolle.BARN }
                ?.map {
                    EyBarn(
                        Foedselsnummer.of(it.relatertPersonsIdent)
                    )
                }
        )
    }


    private fun opprettPerson(
        fnr: Foedselsnummer,
        hentPerson: HentPerson
    ): Person {
        val navn = hentPerson.navn
            .maxByOrNull { it.metadata.sisteRegistrertDato() }!!

        val adressebeskyttelse = hentPerson.adressebeskyttelse
            .any { it.gradering in adressebeskyttet }

        val bostedsadresse = hentPerson.bostedsadresse
            .maxByOrNull { it.metadata.sisteRegistrertDato() }

        val statsborgerskap = hentPerson.statsborgerskap
            .maxByOrNull { it.metadata.sisteRegistrertDato() }

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
            //TODO fjerne adresse fra Person
            adresse = null,
            statsborgerskap = statsborgerskap?.land,
            foedeland = foedsel?.foedeland,
            sivilstatus = sivilstand?.type?.name,
            //TODO endre disse til noe fornuftig
            utland = null,
            rolle = null,
            familieRelasjon = null

        )
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
            //TODO endre disse til noe fornuftig
            utland = null,
            rolle = null,
            familieRelasjon = null

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

    private fun opprettAdresse(adresse: AdresseResponse): eyAdresse {

        //TODO endre logikk for 'paralelle sannheter'
        val bostedsadresse = adresse.data.hentPerson.bostedsadresse
            .maxByOrNull { it.metadata.sisteRegistrertDato() }
        val kontaktsadresse = adresse.data.hentPerson.kontaktadresse
            .maxByOrNull { it.metadata.sisteRegistrertDato() }
        val oppholdssadresse = adresse.data.hentPerson.oppholdsadresse
            .maxByOrNull { it.metadata.sisteRegistrertDato() }

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


    private fun opprettUtland(utland: UtlandResponse): eyUtland {
        return eyUtland(
            utflyttingFraNorge = utland.data?.hentPerson?.utflyttingFraNorge?.map { (mapUtflytting(it)) },
            innflyttingTilNorge = utland.data?.hentPerson?.innflyttingTilNorge?.map { (mapInnflytting(it)) }
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
            dato = innflytting.folkeregistermetadata?.gyldighetstidspunkt
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
