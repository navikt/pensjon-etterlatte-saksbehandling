package no.nav.etterlatte.brev.distribusjon

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.call.body
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.TimeoutForespoerselException
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

class DokDistKanalKlient(
    config: Config,
) {
    private val logger = LoggerFactory.getLogger(DokDistKanalKlient::class.java)

    private val downstreamResourceClient = DownstreamResourceClient(AzureAdClient(config))

    private val url = config.getString("dokdistkanal.resource.url")
    private val clientId = config.getString("dokdistkanal.client.id")

    internal suspend fun bestemDistribusjonskanal(
        request: BestemDistribusjonskanalRequest,
        bruker: BrukerTokenInfo,
    ): BestemDistribusjonskanalResponse =
        try {
            downstreamResourceClient
                .post(Resource(clientId, "$url/bestemDistribusjonskanal"), bruker, request)
                .mapBoth(
                    success = { deserialize(it.response!!.toString()) },
                    failure = {
                        if (it is ResponseException) {
                            when (it.response.status) {
                                HttpStatusCode.OK -> it.response.body()
                                else -> {
                                    logger.error("Fikk statuskode ${it.response.status} fra dokdistkanal: ${it.response.bodyAsText()}")

                                    throw ForespoerselException(
                                        status = it.response.status.value,
                                        code = "UKJENT_FEIL_DOKDIST",
                                        detail = "Ukjent respons fra dokdistkanal",
                                        cause = ResponseException(it.response, "Ukjent feil fra dokdist"),
                                    )
                                }
                            }
                        } else {
                            throw it
                        }
                    },
                )
        } catch (ex: SocketTimeoutException) {
            logger.warn("Timeout mot dokdistkanal", ex)

            throw TimeoutForespoerselException(
                code = "TIMEOUT_DOKDIST",
                detail = "Kallet mot dokdistkanal tok for lang tid. Prøv igjen.",
            )
        }
}

data class BestemDistribusjonskanalRequest(
    val brukerId: String,
    val dokumenttypeId: String?,
    val erArkivert: Boolean,
    val mottakerId: String,
    val tema: String,
)

data class BestemDistribusjonskanalResponse(
    val distribusjonskanal: String,
    val regel: String,
    val regelBegrunnelse: String,
)
