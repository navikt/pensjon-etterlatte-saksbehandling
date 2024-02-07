package no.nav.etterlatte.migrering.pen

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import no.nav.etterlatte.token.Systembruker
import org.slf4j.LoggerFactory

class PenKlient(config: Config, pen: HttpClient) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, pen)

    private val clientId = config.getString("pen.client.id")
    private val resourceUrl = config.getString("pen.client.url")

    suspend fun hentSak(
        sakid: Long,
        lopendeJanuar2024: Boolean = true,
    ): BarnepensjonGrunnlagResponse {
        logger.info("Henter sak $sakid fra PEN")

        return downstreamResourceClient
            .get(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceUrl/barnepensjon-migrering/grunnlag?sakId=$sakid&lopendeJanuar2024=$lopendeJanuar2024",
                    ),
                brukerTokenInfo = Systembruker.migrering,
            )
            .mapBoth(
                success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                failure = { errorResponse -> throw errorResponse },
            )
    }

    suspend fun opphoerSak(pesysId: PesysId) {
        logger.info("Opphører sak $pesysId i PEN")
        downstreamResourceClient
            .post(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceUrl/barnepensjon-migrering/opphoer?sakId=${pesysId.id}",
                    ),
                brukerTokenInfo = Systembruker.migrering,
                postBody = {},
            )
            .mapBoth(
                success = { logger.info("Opphørte sak $pesysId mot PEN") },
                failure = { errorResponse -> throw errorResponse },
            )
    }

    suspend fun sakMedUfoere(fnr: String): List<SakSammendragResponse> {
        logger.info("Henter sak sammendrag for  ${fnr.maskerFnr()} fra PEN")
        return downstreamResourceClient
            .get(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceUrl/sak/sammendragWonderful",
                        additionalHeaders = mapOf("fnr" to fnr),
                    ),
                brukerTokenInfo = Systembruker.migrering,
            )
            .mapBoth(
                success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                failure = { errorResponse -> throw errorResponse },
            )
    }
}
