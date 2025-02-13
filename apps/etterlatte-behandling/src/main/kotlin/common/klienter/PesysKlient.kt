package no.nav.etterlatte.common.klienter

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.LocalDate

interface PesysKlient {
    suspend fun hentSaker(
        fnr: String,
        bruker: BrukerTokenInfo,
    ): List<SakSammendragResponse>
}

class PesysKlientImpl(
    config: Config,
    httpClient: HttpClient,
) : PesysKlient {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("pen.client.id")
    private val resourceUrl = config.getString("pen.client.url")

    override suspend fun hentSaker(
        fnr: String,
        bruker: BrukerTokenInfo,
    ): List<SakSammendragResponse> {
        logger.info("Henter sak sammendrag for  ${fnr.maskerFnr()} fra PEN")

        return downstreamResourceClient
            .get(
                resource =
                    Resource(
                        clientId = clientId,
                        url = "$resourceUrl/sak/sammendrag/v2",
                        additionalHeaders = mapOf("fnr" to fnr),
                    ),
                brukerTokenInfo = bruker,
            ).mapBoth(
                success = { resource -> objectMapper.readValue(resource.response.toString()) },
                failure = { errorResponse -> throw errorResponse },
            )
    }
}

data class SakSammendragResponse(
    val sakType: String,
    val sakStatus: Status,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val fomDato: LocalDate?,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val tomDate: LocalDate?,
) {
    companion object {
        const val UFORE_SAKTYPE = "UFOREP"
        const val ALDER_SAKTYPE = "ALDER"
    }

    enum class Status {
        AVSLUTTET,
        LOPENDE,
        OPPRETTET,
        TIL_BEHANDLING,
    }
}
