package no.nav.etterlatte.samordning.vedtak

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.vedtak.VedtakSamordningDto
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.Systembruker
import org.slf4j.LoggerFactory
import java.time.LocalDate

class VedtakvurderingKlientException(override val message: String, override val cause: Throwable) :
    Exception(message, cause)

class VedtaksvurderingKlient(config: Config, httpClient: HttpClient, azureAdClient: AzureAdClient) {
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)
    private val logger = LoggerFactory.getLogger(VedtaksvurderingKlient::class.java)

    private val clientId = config.getString("vedtak.client_id")
    private val vedtaksvurderingUrl = "${config.getString("vedtak.url")}/api/samordning/vedtak"

    suspend fun hentVedtak(
        vedtakId: Long,
        organisasjonsnummer: String,
    ): VedtakSamordningDto {
        try {
            logger.info("Henter vedtaksvurdering med vedtakId=$vedtakId")

            return downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$vedtaksvurderingUrl/$vedtakId",
                            additionalHeaders = mapOf("orgnr" to organisasjonsnummer),
                        ),
                    brukerTokenInfo = Systembruker(oid = "etterlatte-samordning", sub = "etterlatte-samordning"),
                )
                .mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        } catch (e: Exception) {
            throw VedtakvurderingKlientException(
                "Henting av vedtak med id=$vedtakId feilet",
                e,
            )
        }
    }

    suspend fun hentVedtaksliste(
        virkFom: LocalDate,
        fnr: String,
        organisasjonsnummer: String,
    ): List<VedtakSamordningDto> {
        try {
            logger.info("Henter vedtaksliste, virkFom=$virkFom")

            return downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$vedtaksvurderingUrl?virkFom=$virkFom",
                            additionalHeaders = mapOf("fnr" to fnr, "orgnr" to organisasjonsnummer),
                        ),
                    brukerTokenInfo = Systembruker(oid = "etterlatte-samordning", sub = "etterlatte-samordning"),
                )
                .mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        } catch (e: Exception) {
            throw VedtakvurderingKlientException(
                "Henting av vedtakslisten feilet",
                e,
            )
        }
    }
}
