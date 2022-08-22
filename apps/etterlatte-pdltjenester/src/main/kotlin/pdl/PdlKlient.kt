package no.nav.etterlatte.pdl

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.content.TextContent
import io.ktor.http.ContentType.Application.Json
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.toJson
import org.slf4j.LoggerFactory

class PdlKlient(private val httpClient: HttpClient) {
    private val logger = LoggerFactory.getLogger(PdlKlient::class.java)
    suspend fun hentPerson(fnr: Foedselsnummer, rolle: PersonRolle): PdlPersonResponse {
        val request = PdlGraphqlRequest(
            query = getQuery("/pdl/hentPerson.graphql"),
            variables = toPdlVariables(fnr, rolle)
        )

        return retry<PdlPersonResponse> {
            httpClient.post {
                header("Tema", TEMA)
                accept(Json)
                setBody(TextContent(request.toJson(), Json))
            }.body()
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> throw it.exceptions.last()
            }
        }
    }

    // TODO utvide til rolleliste?
    suspend fun hentPersonBolk(fnr: List<Foedselsnummer>, rolle: PersonRolle): PdlPersonResponseBolk {
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
        logger.info("Bolkhenter personer med fnr=${request.variables.identer} fra PDL")
        return retry<PdlPersonResponseBolk> {
            httpClient.post {
                header("Tema", TEMA)
                accept(Json)
                setBody(TextContent(request.toJson(), Json))
            }.body()
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> throw it.exceptions.last()
            }
        }
    }

    suspend fun hentFolkeregisterIdent(ident: String): PdlFolkeregisterIdentResponse {
        val request = PdlFolkeregisterIdentRequest(
            query = getQuery("/pdl/hentFolkeregisterIdent.graphql"),
            variables = PdlFolkeregisterIdentVariables(
                ident = ident,
                grupper = listOf("FOLKEREGISTERIDENT"),
                historikk = true
            )
        )
        logger.info("Henter folkeregisterident for ident = $ident fra PDL")
        return retry<PdlFolkeregisterIdentResponse> {
            httpClient.post {
                header("Tema", TEMA)
                accept(Json)
                setBody(TextContent(request.toJson(), Json))
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

    private fun toPdlVariables(fnr: Foedselsnummer, rolle: PersonRolle) =
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
        const val TEMA = "PEN"
    }
}