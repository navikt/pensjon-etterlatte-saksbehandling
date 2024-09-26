package no.nav.etterlatte

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import org.slf4j.LoggerFactory

// TODO: legg i modell
class BrevkodeMedData(
    val brevkode: Brevkoder,
    val data: Map<String, Any>,
)

class BrevapiKlient(
    config: Config,
    httpClient: HttpClient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val clientId = config.getString("brevapi.client.id")
    private val baseUrl = config.getString("brevapi.resource.url")
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    internal suspend fun opprettBrev(
        sakid: SakId,
        brevkodeMedData: BrevkodeMedData,
    ) {
        try {
            logger.info("Oppretter brev for sak med sakId=$sakid")
            return downstreamResourceClient
                .post(
                    Resource(clientId, "$baseUrl/api/brev/sak/$sakid/opprettbrevriver"),
                    brukerTokenInfo = HardkodaSystembruker.river,
                    postBody = brevkodeMedData,
                ).mapBoth(
                    success = { true },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        } catch (e: ResponseException) {
            logger.error("Henting av grunnlag for sak med sakId=$sakid feilet", e)

            throw ForespoerselException(
                status = e.response.status.value,
                code = "UKJENT_FEIL_OPPRETTELSE_AV_BREV",
                detail = "Kunne ikke opprette brev for sak: $sakid",
            )
        }
    }
}
