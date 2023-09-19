package no.nav.etterlatte.samordning.vedtak

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.Systembruker
import org.slf4j.LoggerFactory

class VedtakvurderingKlientException(override val message: String, override val cause: Throwable) :
    Exception(message, cause)

class VedtaksvurderingKlient(config: Config, httpClient: HttpClient, azureAdClient: AzureAdClient) {
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)
    private val logger = LoggerFactory.getLogger(VedtaksvurderingKlient::class.java)

    private val clientId = config.getString("vedtak.client_id")
    private val vedtaksvurderingUrl = config.getString("vedtak.url")

    suspend fun hentVedtak(
        vedtakId: Long,
        organisasjonsnummer: String
    ): VedtakDto {
        try {
            logger.info("Henter vedtaksvurdering med vedtakId=$vedtakId")

            return downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$vedtaksvurderingUrl/api/samordning/vedtak/$vedtakId?orgnr=$organisasjonsnummer"
                        ),
                    brukerTokenInfo = Systembruker(oid = "etterlatte-samordning", sub = "etterlatte-samordning")
                )
                .mapBoth(
                    success = { resource ->
                        resource.response.let {
                            objectMapper.readValue(it.toString()) as VedtakDto
                        }
                    },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage }
                )
        } catch (e: Exception) {
            throw VedtakvurderingKlientException(
                "Henting av vedtak med id=$vedtakId feilet",
                e
            )
        }
    }
}