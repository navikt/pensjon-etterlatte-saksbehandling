package no.nav.etterlatte.person

import io.ktor.features.NotFoundException
import no.nav.etterlatte.libs.common.pdl.Gradering

import no.nav.etterlatte.libs.common.pdl.ResponseError
import no.nav.etterlatte.libs.common.person.*
import no.nav.etterlatte.person.pdl.HentPerson
import org.slf4j.LoggerFactory
import person.pdl.InnflyttingTilNorge
import person.pdl.UtflyttingFraNorge
import person.pdl.UtlandResponse

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

    suspend fun hentUtland(fnr: Foedselsnummer): eyUtland {
        logger.info("Henter utland fra PDL")

        val response = klient.hentUtland(fnr)

        val hentUtland: UtlandResponse = response

        //TODO fikse feilhåndtering
        if (!response.errors.isNullOrEmpty()) {
            loggfoerFeilmeldinger(response.errors)
            throw NotFoundException()
        }

        return opprettUtland(hentUtland)
    }

    //TODO suspend her trengs når vi skal reintrodusere kodeverk
    private suspend fun opprettPerson(
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

        //val poststed = kodeverkService.hentPoststed(bostedsadresse?.vegadresse?.postnummer)

        //val land = kodeverkService.hentLand(statsborgerskap?.land)

        return Person(
            fornavn = navn.fornavn,
            etternavn = navn.etternavn,
            foedselsnummer = fnr,
            foedselsdato = foedsel?.foedselsdato?.toString(),
            foedselsaar = foedsel?.foedselsaar,
            doedsdato = doedsfall?.doedsdato.toString(),
            adressebeskyttelse = adressebeskyttelse,
            adresse = bostedsadresse?.vegadresse?.adressenavn,
            husnummer = bostedsadresse?.vegadresse?.husnummer,
            husbokstav = bostedsadresse?.vegadresse?.husbokstav,
            postnummer = bostedsadresse?.vegadresse?.postnummer,
            //TODO introdusere kodeverk igjen
            poststed = bostedsadresse?.vegadresse?.postnummer,
            statsborgerskap = statsborgerskap?.land,
            foedeland = foedsel?.foedeland,
            sivilstatus = sivilstand?.type?.name,
            //TODO endre disse til noe fornuftig
            utland = null,
            rolle = null
        )
    }

    private fun opprettUtland(
        utland: UtlandResponse,
    ): eyUtland {
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
