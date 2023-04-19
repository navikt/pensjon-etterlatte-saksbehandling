package no.nav.etterlatte.pdl

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Behandlingsnummer
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.HentGeografiskTilknytningRequest
import no.nav.etterlatte.libs.common.person.HentPdlIdentRequest
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.PDLIdentGruppeTyper
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.retry
import org.slf4j.LoggerFactory

class PdlKlient(private val httpClient: HttpClient, private val apiUrl: String) {
    private val logger = LoggerFactory.getLogger(PdlKlient::class.java)
    suspend fun hentPerson(hentPersonRequest: HentPersonRequest): PdlPersonResponse {
        val request = PdlGraphqlRequest(
            query = getQuery("/pdl/hentPerson.graphql"),
            variables = toPdlVariables(hentPersonRequest.foedselsnummer, hentPersonRequest.rolle)
        )

        val behandlingsnummer = when (hentPersonRequest.saktype) {
            SakType.BARNEPENSJON -> Behandlingsnummer.BARNEPENSJON
            SakType.OMSTILLINGSSTOENAD -> Behandlingsnummer.OMSTILLINGSSTOENAD
        }

        return retry<PdlPersonResponse>(times = 3) {
            httpClient.post(apiUrl) {
                header(HEADER_BEHANDLINGSNUMMER, behandlingsnummer.behandlingsnummer)
                accept(Json)
                contentType(Json)
                setBody(request)
            }.body()
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> throw it.exceptions.last()
            }
        }
    }

    // TODO utvide til rolleliste?
    suspend fun hentPersonBolk(fnr: List<Folkeregisteridentifikator>, saktype: SakType): PdlPersonResponseBolk {
        val request = PdlGraphqlBolkRequest(
            query = getQuery("/pdl/hentPersonBolk.graphql"),
            variables = PdlBolkVariables(
                identer = fnr.map { it.value },
                bostedsadresse = true,
                bostedsadresseHistorikk = true,
                deltBostedsadresse = true,
                kontaktadresse = true,
                kontaktadresseHistorikk = true,
                oppholdsadresse = true,
                oppholdsadresseHistorikk = true,
                utland = true,
                sivilstand = false,
                familieRelasjon = true,
                vergemaal = true
            )
        )

        val behandlingsnummer = when (saktype) {
            SakType.BARNEPENSJON -> Behandlingsnummer.BARNEPENSJON
            SakType.OMSTILLINGSSTOENAD -> Behandlingsnummer.OMSTILLINGSSTOENAD
        }
        logger.info("Bolkhenter personer med fnr=${request.variables.identer} fra PDL")
        return retry<PdlPersonResponseBolk> {
            httpClient.post(apiUrl) {
                header(HEADER_BEHANDLINGSNUMMER, behandlingsnummer.behandlingsnummer)
                accept(Json)
                contentType(Json)
                setBody(request)
            }.body()
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> throw it.exceptions.last()
            }
        }
    }

    suspend fun hentPdlIdentifikator(request: HentPdlIdentRequest): PdlIdentResponse {
        val graphqlRequest = PdlFolkeregisterIdentRequest(
            query = getQuery("/pdl/hentFolkeregisterIdent.graphql"),
            variables = PdlFolkeregisterIdentVariables(
                ident = request.ident.value,
                grupper = listOf(PDLIdentGruppeTyper.FOLKEREGISTERIDENT.navn, PDLIdentGruppeTyper.NPID.navn),
                historikk = true
            )
        )
        val behandlingsnummer = when (request.saktype) {
            SakType.BARNEPENSJON -> Behandlingsnummer.BARNEPENSJON
            SakType.OMSTILLINGSSTOENAD -> Behandlingsnummer.OMSTILLINGSSTOENAD
        }
        logger.info("Henter PdlIdentifikator for ident = ${request.ident.value} fra PDL")
        return retry<PdlIdentResponse> {
            httpClient.post(apiUrl) {
                header(HEADER_BEHANDLINGSNUMMER, behandlingsnummer.behandlingsnummer)
                accept(Json)
                contentType(Json)
                setBody(graphqlRequest)
            }.body()
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> throw it.exceptions.last()
            }
        }
    }

    suspend fun hentGeografiskTilknytning(request: HentGeografiskTilknytningRequest): PdlGeografiskTilknytningResponse {
        val graphqlRequest = PdlGeografiskTilknytningRequest(
            query = getQuery("/pdl/hentGeografiskTilknytning.graphql"),
            variables = PdlGeografiskTilknytningIdentVariables(
                ident = request.foedselsnummer.value
            )
        )

        logger.info("Henter geografisk tilknytning for fnr = ${request.foedselsnummer.value} fra PDL")
        val behandlingsnummer = when (request.saktype) {
            SakType.BARNEPENSJON -> Behandlingsnummer.BARNEPENSJON
            SakType.OMSTILLINGSSTOENAD -> Behandlingsnummer.OMSTILLINGSSTOENAD
        }
        return retry<PdlGeografiskTilknytningResponse> {
            httpClient.post(apiUrl) {
                header(HEADER_BEHANDLINGSNUMMER, behandlingsnummer.behandlingsnummer)
                accept(Json)
                contentType(Json)
                setBody(graphqlRequest)
            }.body()
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> throw it.exceptions.last()
            }
        }
    }

    private fun getQuery(name: String): String {
        return javaClass.getResource(name)!!
            .readText()
            .replace(Regex("[\n\t]"), "")
    }

    private fun toPdlVariables(fnr: Folkeregisteridentifikator, rolle: PersonRolle) =
        when (rolle) {
            PersonRolle.BARN ->
                PdlVariables(
                    ident = fnr.value,
                    bostedsadresse = true,
                    bostedsadresseHistorikk = true,
                    deltBostedsadresse = true,
                    kontaktadresse = true,
                    kontaktadresseHistorikk = true,
                    oppholdsadresse = true,
                    oppholdsadresseHistorikk = true,
                    utland = true,
                    sivilstand = false,
                    familieRelasjon = true,
                    vergemaal = true
                )
            PersonRolle.GJENLEVENDE ->
                PdlVariables(
                    ident = fnr.value,
                    bostedsadresse = true,
                    bostedsadresseHistorikk = true,
                    deltBostedsadresse = false,
                    kontaktadresse = false,
                    kontaktadresseHistorikk = false,
                    oppholdsadresse = true,
                    oppholdsadresseHistorikk = false,
                    utland = true,
                    sivilstand = true,
                    familieRelasjon = true,
                    vergemaal = false
                )
            PersonRolle.AVDOED ->
                PdlVariables(
                    ident = fnr.value,
                    bostedsadresse = true,
                    bostedsadresseHistorikk = true,
                    deltBostedsadresse = false,
                    kontaktadresse = true,
                    kontaktadresseHistorikk = true,
                    oppholdsadresse = true,
                    oppholdsadresseHistorikk = true,
                    utland = true,
                    sivilstand = true,
                    familieRelasjon = true,
                    vergemaal = false
                )
        }

    companion object {
        const val HEADER_BEHANDLINGSNUMMER = "behandlingsnummer"
    }
}