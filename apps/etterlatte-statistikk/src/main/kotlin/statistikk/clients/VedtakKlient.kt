package no.nav.etterlatte.statistikk.clients

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.Systembruker
import java.util.UUID

class VedtakKlient(
    config: Config,
    httpClient: HttpClient,
) {
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("vedtak.client.id")
    private val resourceUrl = config.getString("vedtak.resource.url")

    fun hentVedtak(
        behandlingId: UUID,
        systembruker: Systembruker,
    ): VedtakDto =
        runBlocking {
            downstreamResourceClient
                .get(
                    Resource(
                        clientId = clientId,
                        url = "$resourceUrl/api/vedtak/$behandlingId",
                    ),
                    systembruker,
                ).mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { errorResponse -> throw errorResponse },
                )
        }
}
