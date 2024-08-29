package no.nav.etterlatte.brev.hentinformasjon.grunnlag

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.OppdaterGrunnlagRequest
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.UUID

class GrunnlagKlient(
    config: Config,
    httpClient: HttpClient,
) {
    private val logger = LoggerFactory.getLogger(GrunnlagKlient::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("grunnlag.client.id")
    private val baseUrl = config.getString("grunnlag.resource.url")

    internal suspend fun hentGrunnlagForSak(
        sakid: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlag {
        try {
            logger.info("Henter grunnlag for sak med sakId=$sakid")

            return downstreamResourceClient
                .get(
                    Resource(clientId, "$baseUrl/api/grunnlag/sak/$sakid"),
                    brukerTokenInfo,
                ).mapBoth(
                    success = { resource -> resource.response.let { deserialize(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        } catch (e: ResponseException) {
            logger.error("Henting av grunnlag for sak med sakId=$sakid feilet", e)

            throw ForespoerselException(
                status = e.response.status.value,
                code = "UKJENT_FEIL_HENT_GRUNNLAG",
                detail = "Henting av grunnlag for sak feilet",
            )
        }
    }

    internal suspend fun hentGrunnlag(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlag {
        try {
            logger.info("Henter grunnlag for behandling med id=$behandlingId")

            return downstreamResourceClient
                .get(
                    Resource(clientId, "$baseUrl/api/grunnlag/behandling/$behandlingId"),
                    brukerTokenInfo,
                ).mapBoth(
                    success = { resource -> resource.response.let { deserialize(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        } catch (e: ResponseException) {
            logger.error("Henting av grunnlag for behandling med id=$behandlingId feilet", e)

            throw ForespoerselException(
                status = e.response.status.value,
                code = "UKJENT_FEIL_HENT_GRUNNLAG",
                detail = "Henting av grunnlag for behandling feilet",
            )
        }
    }

    internal suspend fun oppdaterGrunnlagForSak(
        sak: Sak,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean {
        try {
            logger.info("Oppdaterer grunnlag for sak med id=${sak.id}")

            return downstreamResourceClient
                .post(
                    Resource(clientId, "$baseUrl/api/grunnlag/sak/${sak.id}/oppdater-grunnlag"),
                    brukerTokenInfo,
                    OppdaterGrunnlagRequest(sak.id, sak.sakType),
                ).mapBoth(
                    success = { true },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        } catch (e: ResponseException) {
            logger.error("Oppdatering av grunnlag for sak med id=${sak.id} feilet", e)

            throw ForespoerselException(
                status = e.response.status.value,
                code = "UKJENT_FEIL_OPPDATER_GRUNNLAG",
                detail = "Oppdatering av grunnlag for sak feilet id: ${sak.id}",
            )
        }
    }
}
