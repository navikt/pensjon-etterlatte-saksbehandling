package no.nav.etterlatte.samordning.vedtak

import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import org.slf4j.LoggerFactory
import java.time.LocalDate

class TjenestepensjonKlient(config: Config, httpClient: HttpClient, azureAdClient: AzureAdClient) {
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)
    private val logger = LoggerFactory.getLogger(VedtaksvurderingKlient::class.java)

    private val clientId = config.getString("tjenestepensjon.client_id")
    private val tjenestepensjonUrl = "${config.getString("tjenestepensjon.url")}/api/tjenestepensjon"

    suspend fun harTpForholdByDate(
        fnr: String,
        tpnr: String,
        fomDato: LocalDate,
    ): Boolean {
        logger.info("Sjekk om det finnes tjenestepensjonssforhold pr $fomDato for ordning '$tpnr'")

        return try {
            downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$tjenestepensjonUrl/finnForholdForBruker?datoFom=$fomDato",
                            additionalHeaders =
                                mapOf(
                                    "fnr" to fnr,
                                    "tpnr" to tpnr,
                                ),
                        ),
                    brukerTokenInfo = SamordningSystembruker,
                )
                .mapBoth(
                    success = { deserialize<SamhandlerPersonDto>(it.response.toString()) },
                    failure = { throw it },
                ).forhold.isNotEmpty()
        } catch (e: Exception) {
            throw TjenestepensjonKlientException(
                "Henting av tjenestepensjonsforhold feilet",
                e,
            )
        }
    }

    suspend fun harTpYtelseOnDate(
        fnr: String,
        tpnr: String,
        onDate: LocalDate,
    ): Boolean {
        logger.info("Sjekk om det finnes tjenestepensjonsytelse pr $onDate for ordning '$tpnr'")

        return try {
            downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$tjenestepensjonUrl/haveYtelse?date=$onDate",
                            additionalHeaders =
                                mapOf(
                                    "fnr" to fnr,
                                    "tpnr" to tpnr,
                                ),
                        ),
                    brukerTokenInfo = SamordningSystembruker,
                )
                .mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throw it },
                )
        } catch (e: Exception) {
            throw TjenestepensjonKlientException(
                "Henting av tjenestepensjonsforhold feilet",
                e,
            )
        }
    }
}

class TjenestepensjonKlientException(override val message: String, override val cause: Throwable) :
    Exception(message, cause)
