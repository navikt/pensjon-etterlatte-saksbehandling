package no.nav.etterlatte.pdl

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.content.TextContent
import io.ktor.http.ContentType.Application.Json
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.toJson


class PdlKlient(private val httpClient: HttpClient) {

    suspend fun hentPerson(fnr: Foedselsnummer, rolle: PersonRolle): PdlPersonResponse {
        val request = PdlGraphqlRequest(
            query = getQuery("/pdl/hentPerson.graphql"),
            variables = toPdlVariables(fnr, rolle)
        )

        return retry<PdlPersonResponse> {
            httpClient.post {
                header("Tema", TEMA)
                accept(Json)
                body = TextContent(request.toJson(), Json)
            }
        }.let{
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
                    familieRelasjon = true
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
                    familieRelasjon = true
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
                    familieRelasjon = true
                )
        }

    companion object {
        const val TEMA = "PEN"
    }
}
