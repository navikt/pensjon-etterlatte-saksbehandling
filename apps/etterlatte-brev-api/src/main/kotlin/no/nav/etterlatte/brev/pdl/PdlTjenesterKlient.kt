package no.nav.etterlatte.brev.pdl

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.person.HentPdlIdentRequest
import no.nav.etterlatte.libs.common.person.PersonIdent
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.LocalDate

class PdlTjenesterKlient(
    config: Config,
    httpClient: HttpClient,
) {
    private val logger = LoggerFactory.getLogger(PdlTjenesterKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val url = config.getString("pdltjenester.resource.url")
    private val clientId = config.getString("pdltjenester.client.id")

    suspend fun hentFoedselsdato(
        ident: String,
        brukerTokenInfo: BrukerTokenInfo,
    ): LocalDate? {
        logger.info("Henter ident fra PDL for fnr=${ident.maskerFnr()}")

        return retry<LocalDate?> {
            downstreamResourceClient
                .post(
                    Resource(clientId, "$url/foedselsdato"),
                    brukerTokenInfo,
                    HentPdlIdentRequest(PersonIdent(ident)),
                ).mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throw it },
                )
        }.let { result ->
            when (result) {
                is RetryResult.Success -> result.content
                is RetryResult.Failure -> {
                    logger.error("Feil ved henting av ident fra PDL for fnr=${ident.maskerFnr()}")
                    throw result.samlaExceptions()
                }
            }
        }
    }
}
