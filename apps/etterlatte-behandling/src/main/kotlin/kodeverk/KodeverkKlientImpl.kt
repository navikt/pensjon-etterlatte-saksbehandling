package no.nav.etterlatte.kodeverk

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.SocketTimeoutException
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.feilhaandtering.TimeoutForespoerselException
import no.nav.etterlatte.libs.ktor.Headers
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory

interface KodeverkKlient {
    suspend fun hent(
        kodeverkNavn: KodeverkNavn,
        brukerTokenInfo: BrukerTokenInfo,
    ): KodeverkResponse
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

    override suspend fun hent(
        kodeverkNavn: KodeverkNavn,
        brukerTokenInfo: BrukerTokenInfo,
    ): KodeverkResponse =
        try {
            logger.info("Henter alle landkoder fra Kodeverk")
            downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$url/$kodeverkNavn/koder/betydninger?ekskluderUgyldige=false&spraak=nb",
                            additionalHeaders = mapOf(Headers.NAV_CONSUMER_ID to applicationName),
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                ).mapBoth(
                    success = { resource -> deserialize(resource.response.toString()) },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        } catch (e: SocketTimeoutException) {
            logger.warn("Timeout mot kodeverk ved henting av landkoder")

            throw TimeoutForespoerselException(
                code = "KODEVERK_TIMEOUT",
                detail = "Henting av ${kodeverkNavn.verdi} fra kodeverk tok for lang tid... Prøv igjen om litt.",
            )
        } catch (e: Exception) {
            logger.error("Henting av landkoder feilet", e)
            throw e
        }
}

enum class KodeverkNavn(
    val verdi: String,
) {
    ARKIVTEMAER("Arkivtemaer"),
    LANDKODER("Landkoder"),
    LANDKODERISO2("LandkoderISO2"),
    ;

    override fun toString(): String = verdi
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
