package no.nav.etterlatte.kodeverk

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.SocketTimeoutException
import no.nav.etterlatte.libs.common.feilhaandtering.TimeoutForespoerselException
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktor.Headers
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

interface KodeverkKlient {
    suspend fun hentLandkoder(brukerTokenInfo: BrukerTokenInfo): KodeverkResponse

    suspend fun hentArkivTemaer(brukerTokenInfo: BrukerTokenInfo): KodeverkResponse
}

class KodeverkKlientImpl(
    config: Config,
    httpKlient: HttpClient,
) : KodeverkKlient {
    private val logger = LoggerFactory.getLogger(KodeverkKlientImpl::class.java)
    private val url = config.getString("kodeverk.resource.url")
    private val clientId = config.getString("kodeverk.client.id")
    private val applicationName = config.getString("applicationName")
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpKlient)

    override suspend fun hentLandkoder(brukerTokenInfo: BrukerTokenInfo): KodeverkResponse =
        try {
            logger.info("Henter alle landkoder fra Kodeverk")
            downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$url/Landkoder/koder/betydninger?ekskluderUgyldige=false&spraak=nb",
                            additionalHeaders = mapOf(Headers.NAV_CONSUMER_ID to applicationName),
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                ).mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        } catch (e: SocketTimeoutException) {
            logger.warn("Timeout mot kodeverk ved henting av landkoder")

            throw TimeoutForespoerselException(
                code = "KODEVERK_TIMEOUT",
                detail = "Henting av landkoder fra kodeverk tok for lang tid... Prøv igjen om litt.",
            )
        } catch (e: Exception) {
            logger.error("Henting av landkoder feilet", e)
            throw e
        }

    override suspend fun hentArkivTemaer(brukerTokenInfo: BrukerTokenInfo): KodeverkResponse =
        try {
            logger.info("Henter arkivtemaer fra Kodeverk")
            downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$url/Arkivtemaer/koder/betydninger?ekskluderUgyldige=true&spraak=nb",
                            additionalHeaders = mapOf(Headers.NAV_CONSUMER_ID to applicationName),
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                ).mapBoth(
                    success = { resource ->
                        resource.response.let { objectMapper.readValue<KodeverkResponse>(it.toString()) }
                    },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        } catch (e: SocketTimeoutException) {
            logger.warn("Timeout mot kodeverk ved henting av arkivtemaer")

            throw TimeoutForespoerselException(
                code = "KODEVERK_TIMEOUT",
                detail = "Henting av arkivtemaer fra kodeverk tok for lang tid... Prøv igjen om litt.",
            )
        } catch (e: Exception) {
            logger.error("Henting av arkivtemaer feilet", e)
            throw e
        }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class KodeverkResponse(
    val betydninger: Map<String, List<Betydning>>,
)

data class Beskrivelse(
    val term: String,
    val tekst: String,
)

data class Betydning(
    val gyldigFra: String,
    val gyldigTil: String,
    val beskrivelser: Map<String, Beskrivelse>,
)

data class BetydningMedIsoKode(
    val gyldigFra: String,
    val gyldigTil: String,
    val beskrivelser: Map<String, Beskrivelse>,
    val isolandkode: String,
)
