package no.nav.etterlatte.trygdetid.avtale

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.trygdetid.avtale.Trygdeavtale
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import org.slf4j.LoggerFactory
import java.util.UUID

class AvtaleRepositoryKlient(
    config: Config,
    httpClient: HttpClient,
) : AvtaleRepositoryOperasjoner {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("trygdetid.client.id")
    private val resourceUrl = config.getString("trygdetid.resource.url")

    private val baseUrl get() = "$resourceUrl/intern/trygdetid-crud/avtale"
    private val systembruker = HardkodaSystembruker.river

    override fun hentAvtale(behandlingId: UUID): Trygdeavtale? =
        runBlocking {
            try {
                getOrNull("$baseUrl/$behandlingId")
            } catch (e: Exception) {
                throw AvtaleRepositoryKlientException("Hent avtale for behandling=$behandlingId feilet", e)
            }
        }

    override fun lagreAvtale(trygdeavtale: Trygdeavtale) {
        runBlocking {
            try {
                logger.info("Lagrer avtale via klient for behandling=${trygdeavtale.behandlingId}")
                put("$baseUrl/lagre", trygdeavtale)
            } catch (e: Exception) {
                throw AvtaleRepositoryKlientException(
                    "Lagre avtale feilet for behandling=${trygdeavtale.behandlingId}",
                    e,
                )
            }
        }
    }

    override fun opprettAvtale(trygdeavtale: Trygdeavtale) {
        runBlocking {
            try {
                logger.info("Oppretter avtale via klient for behandling=${trygdeavtale.behandlingId}")
                post("$baseUrl/opprett", trygdeavtale)
            } catch (e: Exception) {
                throw AvtaleRepositoryKlientException(
                    "Opprett avtale feilet for behandling=${trygdeavtale.behandlingId}",
                    e,
                )
            }
        }
    }

    private suspend inline fun <reified T : Any> getOrNull(url: String): T? =
        try {
            downstreamResourceClient
                .get(
                    resource = Resource(clientId = clientId, url = url),
                    brukerTokenInfo = systembruker,
                ).mapBoth(
                    success = { resource -> objectMapper.readValue<T>(resource.response.toString()) },
                    failure = { errorResponse -> throw errorResponse },
                )
        } catch (e: ResponseException) {
            if (e.response.status == HttpStatusCode.NotFound) null else throw e
        }

    private suspend fun post(
        url: String,
        body: Any,
    ) {
        downstreamResourceClient
            .post(
                resource = Resource(clientId = clientId, url = url),
                brukerTokenInfo = systembruker,
                postBody = body,
            ).mapBoth(
                success = { },
                failure = { errorResponse -> throw errorResponse },
            )
    }

    private suspend fun put(
        url: String,
        body: Any,
    ) {
        downstreamResourceClient
            .put(
                resource = Resource(clientId = clientId, url = url),
                brukerTokenInfo = systembruker,
                putBody = body,
            ).mapBoth(
                success = { },
                failure = { errorResponse -> throw errorResponse },
            )
    }
}

class AvtaleRepositoryKlientException(
    override val message: String,
    override val cause: Throwable,
) : Exception(message, cause)
