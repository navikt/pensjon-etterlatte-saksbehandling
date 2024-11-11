package no.nav.etterlatte.pdl

import com.github.michaelbull.result.get
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.ktor.behandlingsnummer
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdHttpClient
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.utils.toPdlSearchVariables
import no.nav.etterlatte.utils.toPdlVariables
import java.time.LocalDate

data class SoekPerson(
    val navn: String,
    val foedselsdato: LocalDate,
)

class PdlOboKlient(
    private val httpClient: HttpClient,
    config: Config,
) {
    private val apiUrl = config.getString("pdl.url")
    private val pdlScope = config.getString("pdl.scope")

    private val azureAdClient = AzureAdClient(config, AzureAdHttpClient(httpClient))

    suspend fun hentPersonNavnOgFoedsel(
        ident: String,
        bruker: BrukerTokenInfo,
    ): PdlPersonNavnFoedselResponse {
        val request =
            PdlGraphqlRequest(
                query = getQuery("/pdl/hentPersonNavnFoedsel.graphql"),
                variables = PdlVariables(ident),
            )

        return retry<PdlPersonNavnFoedselResponse>(times = 3) {
            httpClient
                .post(apiUrl) {
                    bearerAuth(getOboToken(bruker))
                    behandlingsnummer(SakType.entries)
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(request)
                }.body()
        }.let {
            when (it) {
                is RetryResult.Success ->
                    it.content.also { result -> loggDelvisReturnerteData(result, request) }

                is RetryResult.Failure -> throw it.samlaExceptions()
            }
        }
    }

    suspend fun hentPerson(
        fnr: Folkeregisteridentifikator,
        rolle: PersonRolle,
        sakType: SakType,
        bruker: BrukerTokenInfo,
    ): PdlPersonResponse {
        val request =
            PdlGraphqlRequest(
                query = getQuery("/pdl/hentPerson.graphql"),
                variables = toPdlVariables(fnr, rolle),
            )

        return retry<PdlPersonResponse>(times = 3) {
            httpClient
                .post(apiUrl) {
                    bearerAuth(getOboToken(bruker))
                    behandlingsnummer(sakType)
                    header(PdlKlient.HEADER_TEMA, PdlKlient.HEADER_TEMA_VALUE)
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body()
        }.let {
            when (it) {
                is RetryResult.Success ->
                    it.content.also { result -> loggDelvisReturnerteData(result, request) }

                is RetryResult.Failure -> throw it.samlaExceptions()
            }
        }
    }

    suspend fun soekPerson(
        soekPerson: SoekPerson,
        bruker: BrukerTokenInfo,
    ): PdlPersonSoekResponse {
        val request =
            PdlGraphSoekRequest(
                query = getQuery("/pdl/soekPerson.graphql"),
                variables = toPdlSearchVariables(soekPerson),
            )

        return retry<PdlPersonSoekResponse>(times = 3) {
            httpClient
                .post(apiUrl) {
                    bearerAuth(getOboToken(bruker))
                    header(PdlKlient.HEADER_TEMA, PdlKlient.HEADER_TEMA_VALUE)
                    behandlingsnummer(SakType.entries)
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body()
        }.let {
            when (it) {
                is RetryResult.Success ->
                    it.content.also { result -> loggDelvisReturnerteData(result, request) }

                is RetryResult.Failure -> throw it.samlaExceptions()
            }
        }
    }

    private fun getQuery(name: String): String =
        javaClass
            .getResource(name)!!
            .readText()
            .replace(Regex("[\n\t]"), "")

    private suspend fun getOboToken(bruker: BrukerTokenInfo): String {
        val token =
            azureAdClient.hentTokenFraAD(
                bruker,
                listOf(pdlScope),
            )

        return requireNotNull(token.get()?.accessToken) {
            "Kunne ikke hente ut obo-token for bruker ${bruker.ident()}"
        }
    }
}
