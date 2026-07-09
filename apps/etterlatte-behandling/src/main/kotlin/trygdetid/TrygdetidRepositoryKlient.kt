package no.nav.etterlatte.trygdetid

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import org.slf4j.LoggerFactory
import java.util.UUID

class TrygdetidRepositoryKlient(
    config: Config,
    httpClient: HttpClient,
) : TrygdetidRepositoryOperasjoner {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("trygdetid.client.id")
    private val resourceUrl = config.getString("trygdetid.resource.url")

    private val baseUrl get() = "$resourceUrl/intern/trygdetid-crud"
    private val systembruker = HardkodaSystembruker.river

    override fun hentTrygdetidMedId(
        behandlingId: UUID,
        trygdetidId: UUID,
    ): Trygdetid? =
        runBlocking {
            try {
                getOrNull("$baseUrl/behandling/$behandlingId/$trygdetidId")
            } catch (e: Exception) {
                throw TrygdetidRepositoryKlientException(
                    "Hent trygdetid med id=$trygdetidId for behandling=$behandlingId feilet",
                    e,
                )
            }
        }

    override fun hentTrygdetid(behandlingId: UUID): Trygdetid? = hentTrygdetiderForBehandling(behandlingId).minByOrNull { it.ident }

    override fun hentTrygdetiderForBehandling(behandlingId: UUID): List<Trygdetid> =
        runBlocking {
            try {
                get("$baseUrl/behandling/$behandlingId")
            } catch (e: Exception) {
                throw TrygdetidRepositoryKlientException(
                    "Hent trygdetider for behandling=$behandlingId feilet",
                    e,
                )
            }
        }

    override fun opprettTrygdetid(trygdetid: Trygdetid): Trygdetid =
        runBlocking {
            try {
                logger.info("Oppretter trygdetid via klient for behandling=${trygdetid.behandlingId}")
                post("$baseUrl/opprett", trygdetid)
            } catch (e: Exception) {
                throw TrygdetidRepositoryKlientException(
                    "Opprett trygdetid feilet for behandling=${trygdetid.behandlingId}",
                    e,
                )
            }
        }

    override fun oppdaterTrygdetid(oppdatertTrygdetid: Trygdetid): Trygdetid =
        runBlocking {
            try {
                logger.info("Oppdaterer trygdetid via klient for behandling=${oppdatertTrygdetid.behandlingId}")
                put("$baseUrl/oppdater", oppdatertTrygdetid)
            } catch (e: Exception) {
                throw TrygdetidRepositoryKlientException(
                    "Oppdater trygdetid feilet for behandling=${oppdatertTrygdetid.behandlingId}",
                    e,
                )
            }
        }

    override fun slettTrygdetid(trygdetidId: UUID) {
        runBlocking {
            try {
                logger.info("Sletter trygdetid via klient med id=$trygdetidId")
                delete("$baseUrl/$trygdetidId")
            } catch (e: Exception) {
                throw TrygdetidRepositoryKlientException("Slett trygdetid feilet for id=$trygdetidId", e)
            }
        }
    }

    override fun hentTrygdetiderForAvdoede(avdoede: List<String>): List<TrygdetidPartial> =
        runBlocking {
            try {
                post("$baseUrl/avdoede", HentTrygdetiderForAvdoedeRequest(avdoede))
            } catch (e: Exception) {
                throw TrygdetidRepositoryKlientException("Hent trygdetider for avdøde feilet", e)
            }
        }

    private suspend inline fun <reified T> get(url: String): T =
        downstreamResourceClient
            .get(
                resource = Resource(clientId = clientId, url = url),
                brukerTokenInfo = systembruker,
            ).mapBoth(
                success = { resource -> objectMapper.readValue(resource.response.toString()) },
                failure = { errorResponse -> throw errorResponse },
            )

    private suspend inline fun <reified T : Any> getOrNull(url: String): T? =
        try {
            get(url)
        } catch (e: ResponseException) {
            if (e.response.status == HttpStatusCode.NotFound) null else throw e
        }

    private suspend inline fun <reified T> post(
        url: String,
        body: Any,
    ): T =
        downstreamResourceClient
            .post(
                resource = Resource(clientId = clientId, url = url),
                brukerTokenInfo = systembruker,
                postBody = body,
            ).mapBoth(
                success = { resource -> objectMapper.readValue(resource.response.toString()) },
                failure = { errorResponse -> throw errorResponse },
            )

    private suspend inline fun <reified T> put(
        url: String,
        body: Any,
    ): T =
        downstreamResourceClient
            .put(
                resource = Resource(clientId = clientId, url = url),
                brukerTokenInfo = systembruker,
                putBody = body,
            ).mapBoth(
                success = { resource -> objectMapper.readValue(resource.response.toString()) },
                failure = { errorResponse -> throw errorResponse },
            )

    private suspend fun delete(url: String) {
        downstreamResourceClient
            .delete(
                resource = Resource(clientId = clientId, url = url),
                brukerTokenInfo = systembruker,
            ).mapBoth(
                success = { },
                failure = { errorResponse -> throw errorResponse },
            )
    }
}

data class HentTrygdetiderForAvdoedeRequest(
    val avdoede: List<String>,
)

class TrygdetidRepositoryKlientException(
    override val message: String,
    override val cause: Throwable,
) : Exception(message, cause)
