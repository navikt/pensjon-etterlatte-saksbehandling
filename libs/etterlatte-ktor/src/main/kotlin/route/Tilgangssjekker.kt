package no.nav.etterlatte.libs.ktor.route

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.UUID

class Tilgangssjekker(
    config: Config,
    httpClient: HttpClient,
) : BehandlingTilgangsSjekk,
    SakTilgangsSjekk,
    PersonTilgangsSjekk {
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")

    private val tilgangscache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .build<Tilgangsrequest, Boolean>()

    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun harTilgangTilBehandling(
        behandlingId: UUID,
        skrivetilgang: Boolean,
        bruker: Saksbehandler,
    ): Boolean {
        val tilgangsrequest = Tilgangsrequest(behandlingId, skrivetilgang, bruker.ident)
        val fraCache = tilgangscache.getIfPresent(tilgangsrequest)
        if (fraCache != null) {
            logger.debug("Cache hit for behandling")
            return fraCache
        }
        logger.debug("Cache miss for behandling")
        try {
            logger.debug("Sjekker tilgang til behandling {}", behandlingId)
            return downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/tilgang/behandling/$behandlingId?skrivetilgang=$skrivetilgang",
                        ),
                    brukerTokenInfo = bruker,
                ).mapBoth(
                    success = { resource ->
                        resource.response
                            .let { objectMapper.readValue<Boolean>(it.toString()) }
                            .also { tilgangscache.put(tilgangsrequest, it) }
                    },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        } catch (e: Exception) {
            throw TilgangssjekkException("Sjekking av tilgang for behandling feilet", e)
        }
    }

    override suspend fun harTilgangTilPerson(
        foedselsnummer: Folkeregisteridentifikator,
        skrivetilgang: Boolean,
        bruker: Saksbehandler,
    ): Boolean {
        val tilgangsrequest = Tilgangsrequest(foedselsnummer, skrivetilgang, bruker.ident)
        val fraCache = tilgangscache.getIfPresent(tilgangsrequest)
        if (fraCache != null) {
            logger.debug("Cache hit for person")
            return fraCache
        }
        logger.debug("Cache miss for person")
        try {
            return downstreamResourceClient
                .post(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/tilgang/person?skrivetilgang=$skrivetilgang",
                        ),
                    brukerTokenInfo = bruker,
                    postBody = foedselsnummer.value,
                ).mapBoth(
                    success = { resource ->
                        resource.response
                            .let { objectMapper.readValue<Boolean>(it.toString()) }
                            .also { tilgangscache.put(tilgangsrequest, it) }
                    },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        } catch (e: Exception) {
            throw TilgangssjekkException("Sjekking av tilgang for person feilet", e)
        }
    }

    override suspend fun harTilgangTilSak(
        sakId: Long,
        skrivetilgang: Boolean,
        bruker: Saksbehandler,
    ): Boolean {
        val tilgangsrequest = Tilgangsrequest(sakId, skrivetilgang, bruker.ident)
        val fraCache = tilgangscache.getIfPresent(tilgangsrequest)
        if (fraCache != null) {
            logger.debug("Cache hit for sak")
            return fraCache
        }
        logger.debug("Cache miss for sak")
        try {
            return downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/tilgang/sak/$sakId?skrivetilgang=$skrivetilgang",
                        ),
                    brukerTokenInfo = bruker,
                ).mapBoth(
                    success = {
                        deserialize<Boolean>(it.response.toString())
                            .also { resultat -> tilgangscache.put(tilgangsrequest, resultat) }
                    },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        } catch (e: Exception) {
            throw TilgangssjekkException("Sjekking av tilgang for sak feilet", e)
        }
    }
}

class TilgangssjekkException(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause)

data class Tilgangsrequest(
    val id: Any,
    val skrivetilgang: Boolean,
    val bruker: String,
)
