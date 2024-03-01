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
import io.ktor.http.HttpMessageBuilder
import io.ktor.http.contentType
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.Behandlingsnummer
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

class PdlOboKlient(private val httpClient: HttpClient, private val config: Config) {
    private val logger = LoggerFactory.getLogger(PdlKlient::class.java)

    private val apiUrl = config.getString("pdl.url")
    private val pdlScope = config.getString("pdl.scope")

    private val azureAdClient = AzureAdClient(config, httpClient)

    suspend fun hentPersonNavn(
        ident: String,
        bruker: BrukerTokenInfo,
    ): PdlPersonResponse {
        val request =
            PdlGraphqlRequest(
                query = getQuery("/pdl/hentPerson.graphql"),
                variables = PdlVariables(ident),
            )

        return retry<PdlPersonResponse>(times = 3) {
            httpClient.post(apiUrl) {
                bearerAuth(getOboToken(bruker))
                behandlingsnummer(Behandlingsnummer.BARNEPENSJON, Behandlingsnummer.OMSTILLINGSSTOENAD)
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(request)
            }.body()
        }.let {
            when (it) {
                is RetryResult.Success ->
                    it.content.also { result ->
                        result.errors?.joinToString()?.let { feil ->
                            logger.error("Fikk data fra PDL, men også følgende feil: $feil")
                        }
                    }

                is RetryResult.Failure -> throw it.samlaExceptions()
            }
        }
    }

    private fun getQuery(name: String): String {
        return javaClass.getResource(name)!!
            .readText()
            .replace(Regex("[\n\t]"), "")
    }

    private suspend fun getOboToken(bruker: BrukerTokenInfo): String {
        val token =
            azureAdClient.getOnBehalfOfAccessTokenForResource(
                listOf(pdlScope),
                bruker.accessToken(),
            )

        return requireNotNull(token.get()?.accessToken) {
            "Kunne ikke hente ut obo-token for bruker ${bruker.ident()}"
        }
    }

    public fun HttpMessageBuilder.behandlingsnummer(vararg behandlingsnummer: Behandlingsnummer): Unit =
        header("behandlingsnummer", behandlingsnummer.joinToString { it.behandlingsnummer })
}
