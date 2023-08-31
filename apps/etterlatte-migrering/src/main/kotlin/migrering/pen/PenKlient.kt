package no.nav.etterlatte.migrering.pen

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.objectMapper
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
    fun hentAlleSaker(): List<PesysId> {
        // TODO i EY-2599: Kople opp mot PEN/Pesys her
        return listOf()
    }
    suspend fun hentSak(sakid: Long): BarnepensjonGrunnlagResponse {
        logger.info("Henter sak $sakid fra PEN")

        return downstreamResourceClient
            .get(
                resource = Resource(
                    clientId = clientId,
                    url = "$resourceUrl/barnepensjon-migrering/grunnlag?sakId=$sakid"
                ),
                brukerTokenInfo = Systembruker(oid = "TODO", sub = "TODO")
            )
            .mapBoth(
                success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                failure = { errorResponse -> throw errorResponse }
            )
    }
}