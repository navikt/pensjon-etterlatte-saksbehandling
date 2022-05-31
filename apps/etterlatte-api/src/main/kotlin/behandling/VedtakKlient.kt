package no.nav.etterlatte.behandling

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.KommerSoekerTilgode
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory
import java.util.*


interface EtterlatteVedtak {
    suspend fun hentVedtak(sakId: Int, behandlingId: String, accessToken: String): Vedtak
}

class VedtakKlient(config: Config, httpClient: HttpClient) : EtterlatteVedtak {
    private val logger = LoggerFactory.getLogger(VedtakKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("vedtak.client.id")
    private val resourceUrl = config.getString("vedtak.resource.url")


    companion object {
        fun serialize(data: Any): String {
            return objectMapper.writeValueAsString(data)
        }
    }


    override suspend fun hentVedtak(sakId: Int, behandlingId: String, accessToken: String): Vedtak {
        logger.info("Henter vedtak for en behandling")

        try {
            val json =
                downstreamResourceClient.get(Resource(clientId, "$resourceUrl/api/hentvedtak/$sakId/$behandlingId"), accessToken)
                    .mapBoth(
                        success = { json -> json },
                        failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                    ).response
            return objectMapper.readValue(json.toString())
        } catch (e: Exception) {
            logger.error("Henting  vedtak for en behandling feilet", e)
            throw e
        }    }


}

data class Vedtak(
    val sakId: String,
    val behandlingId: UUID,
    val saksbehandlerId: String?,
    val avkortingsResultat: String?,
    val beregningsResultat: BeregningsResultat?,
    val vilkaarsResultat: VilkaarResultat?,
    val kommerSoekerTilgodeResultat: KommerSoekerTilgode?,
    val vedtakFattet: Boolean?
)