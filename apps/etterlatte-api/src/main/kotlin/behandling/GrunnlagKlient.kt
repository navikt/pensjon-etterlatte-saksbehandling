package no.nav.etterlatte.behandling

import com.github.michaelbull.result.mapBoth
import io.ktor.client.*
import com.typesafe.config.Config
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory


interface EtterlatteGrunnlag {
    suspend fun lagreResultatKommerBarnetTilgode(sakId: Int, behandlingsId: String, opplysning: List<Grunnlagsopplysning<out Any>>, accessToken: String)
}

class GrunnlagKlient(config: Config, httpClient: HttpClient) : EtterlatteGrunnlag {
    private val logger = LoggerFactory.getLogger(GrunnlagKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("grunnlag.client.id")
    private val resourceUrl = config.getString("grunnlag.resource.url")

    override suspend fun lagreResultatKommerBarnetTilgode(sakId: Int, behandlingsId: String, opplysning: List<Grunnlagsopplysning<out Any>>, accessToken: String) {
        logger.info("Lagrer resultat om pensjon kommer barnet tilgode med behandlingsId: $behandlingsId")
        try {
            downstreamResourceClient.post(
                Resource(clientId, "$resourceUrl/api/kommerbarnettilgode"),
                accessToken,
                SaksbehandlerOpplysning(sakId.toString(), behandlingsId, opplysning)

            ).mapBoth(
                success = { json -> json },
                failure = { throwableErrorMessage -> throw Error(throwableErrorMessage.message) }
            ).response
        } catch (e: Exception) {
            logger.error("Lagring av pensjon kommer barnet til gode feilet", e)
            throw e
        }
    }
}

data class SaksbehandlerOpplysning(val sakId: String, val behandlingId: String, val opplysning: List<Grunnlagsopplysning<out Any>>)
