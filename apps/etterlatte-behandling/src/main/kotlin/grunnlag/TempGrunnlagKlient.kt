package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.NyePersonopplysninger
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

class TempGrunnlagKlient(
    config: Config,
    httpClient: HttpClient,
) {
    private val logger = LoggerFactory.getLogger(GrunnlagKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("grunnlag.client.id")
    private val resourceUrl = config.getString("grunnlag.resource.url")
    private val url = "$resourceUrl/api"

    suspend fun lagreNyePersonopplysninger(
        sakId: SakId,
        behandlingId: UUID,
        fnr: Folkeregisteridentifikator,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        logger.info("Lagrer nye personopplysninger på sak $sakId")

        downstreamResourceClient
            .post(
                Resource(clientId, "$url/grunnlag/person/behandling/$behandlingId/nye-opplysninger"),
                brukerTokenInfo,
                postBody = NyePersonopplysninger(sakId, fnr, nyeOpplysninger),
            ).mapBoth(
                success = {
                    logger.info("Personopplysninger lagret på sak $sakId")
                },
                failure = { throw it },
            )
    }

    suspend fun lagreNyeSaksopplysninger(
        sakId: SakId,
        behandlingId: UUID,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        logger.info("Lagrer nye saksopplysninger på sak $sakId")

        downstreamResourceClient
            .post(
                Resource(clientId, "$url/grunnlag/behandling/$behandlingId/nye-opplysninger"),
                brukerTokenInfo,
                postBody = NyeSaksopplysninger(sakId, nyeOpplysninger),
            ).mapBoth(
                success = {
                    logger.info("Saksopplysninger lagret på sak $sakId")
                },
                failure = { throw it },
            )
    }

    suspend fun laasVersjonForBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        logger.info("Låser grunnlagsversjon for behandling (id=$behandlingId)")

        downstreamResourceClient
            .post(
                Resource(clientId, "$url/grunnlag/behandling/$behandlingId/laas"),
                brukerTokenInfo,
                postBody = { },
            ).mapBoth(
                success = {
                    logger.info("Grunnlagsversjon for behandling ble låst (behandlingId=$behandlingId)")
                },
                failure = { throw it },
            )
    }
}
