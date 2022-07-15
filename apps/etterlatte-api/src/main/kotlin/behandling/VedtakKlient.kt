package no.nav.etterlatte.behandling

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.avkorting.AvkortingsResultat
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.KommerSoekerTilgode
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.util.*


interface EtterlatteVedtak {
    suspend fun hentVedtak(sakId: Int, behandlingId: String, accessToken: String): Vedtak
    suspend fun fattVedtak(sakId: Int, behandlingId: String, accessToken: String)
    suspend fun attesterVedtak(sakId: Int, behandlingId: String, accessToken: String)
    suspend fun underkjennVedtak(sakId: Int, behandlingId: String, begrunnelse: String, kommentar: String, accessToken: String)
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
                downstreamResourceClient.get(
                    Resource(clientId, "$resourceUrl/api/hentvedtak/$sakId/$behandlingId"),
                    accessToken
                )
                    .mapBoth(
                        success = { json -> json },
                        failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                    ).response
            return objectMapper.readValue(json.toString())
        } catch (e: Exception) {
            logger.error("Henting  vedtak for en behandling feilet", e)
            throw e
        }
    }


    override suspend fun fattVedtak(sakId: Int, behandlingId: String, accessToken: String) {
        logger.info("Fatter vedtak")
        try {
            downstreamResourceClient.post(
                Resource(clientId, "$resourceUrl/api/fattVedtak"),
                accessToken,
                FattVedtakBody(sakId.toString(), behandlingId)
            )
                .mapBoth(
                    success = { json -> json },
                    failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
                ).response
        } catch (e: Exception) {
            logger.error("Fatting av vedtak feilet", e)
            throw e
        }
    }

    override suspend fun attesterVedtak(sakId: Int, behandlingId: String, accessToken: String) {
        logger.info("Attesterer vedtak")
        try {
            downstreamResourceClient.post(
                Resource(clientId, "$resourceUrl/api/attesterVedtak"),
                accessToken,
                FattVedtakBody(sakId.toString(), behandlingId)
            ).mapBoth(
                success = { json -> json },
                failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
            ).response
        } catch (e: Exception) {
            logger.error("Attestering av vedtak feilet", e)
            throw e
        }
    }

    override suspend fun underkjennVedtak(sakId: Int, behandlingId: String, begrunnelse: String, kommentar: String, accessToken: String) {
        logger.info("Underkjenner vedtak med id ", behandlingId)
        try {
            downstreamResourceClient.post(
                Resource(clientId, "$resourceUrl/api/underkjennVedtak"),
                accessToken,
                UnderkjennVedtakBody(sakId.toString(), behandlingId, kommentar, begrunnelse)
            ).mapBoth(
                success = { json -> json },
                failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
            ).response
        } catch (e: Exception) {
            logger.error("Underkjenning av vedtak feilet", e)
            throw e
        }
    }

}

data class Vedtak(
    val sakId: String,
    val behandlingId: UUID,
    val saksbehandlerId: String?,
    val avkortingsResultat: AvkortingsResultat?,
    val beregningsResultat: BeregningsResultat?,
    val vilkaarsResultat: VilkaarResultat?,
    val kommerSoekerTilgodeResultat: KommerSoekerTilgode?,
    val virkningsDato: LocalDate?,
    val vedtakFattet: Boolean?,
    val datoFattet: Instant?,
    val datoattestert: Instant?,
    val attestant: String?,
)

data class FattVedtakBody(val sakId: String, val behandlingId: String)
data class UnderkjennVedtakBody(val sakId: String, val behandlingId: String, val kommentar: String, val valgtBegrunnelse: String)
