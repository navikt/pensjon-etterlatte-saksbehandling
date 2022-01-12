package no.nav.etterlatte.person

import io.ktor.features.NotFoundException
import no.nav.etterlatte.libs.common.pdl.Gradering

import no.nav.etterlatte.libs.common.pdl.ResponseError
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.person.pdl.HentPerson
import org.slf4j.LoggerFactory

class PersonService(
    private val klient: PersonKlient

) {
    private val logger = LoggerFactory.getLogger(PersonService::class.java)
    private val adressebeskyttet = listOf(Gradering.FORTROLIG, Gradering.STRENGT_FORTROLIG,
        Gradering.STRENGT_FORTROLIG_UTLAND
    )

    suspend fun hentPerson(fnr: Foedselsnummer): Person {
        logger.info("Henter person fra PDL")

        val response = klient.hentPerson(fnr)

        val hentPerson = response.data?.hentPerson

        if (hentPerson == null) {
            loggfoerFeilmeldinger(response.errors)
            throw NotFoundException()
        }

        return opprettPerson(fnr, hentPerson)
    }

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

        //val poststed = kodeverkService.hentPoststed(bostedsadresse?.vegadresse?.postnummer)

        //val land = kodeverkService.hentLand(statsborgerskap?.land)

        return Person(
            fornavn = navn.fornavn,
            etternavn = navn.etternavn,
            foedselsnummer = fnr,
            foedselsdato = foedsel?.foedselsdato?.toString(),
            foedselsaar = foedsel?.foedselsaar,
            adressebeskyttelse = adressebeskyttelse,
            adresse = bostedsadresse?.vegadresse?.adressenavn,
            husnummer = bostedsadresse?.vegadresse?.husnummer,
            husbokstav = bostedsadresse?.vegadresse?.husbokstav,
            postnummer = bostedsadresse?.vegadresse?.postnummer,
            //TODO introdusere kodeverk igjen
            poststed = bostedsadresse?.vegadresse?.postnummer,
            statsborgerskap = statsborgerskap?.land,
            foedeland = foedsel?.foedeland,
            sivilstatus = sivilstand?.type?.name
        )
    }

    private fun loggfoerFeilmeldinger(errors: List<ResponseError>?) {
        logger.error("Kunne ikke hente person fra PDL")

        errors?.forEach {
            logger.error(it.message)
        }
    }
}
