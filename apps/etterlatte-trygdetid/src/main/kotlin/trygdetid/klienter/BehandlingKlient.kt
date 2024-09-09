package no.nav.etterlatte.trygdetid.klienter

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.SisteIverksatteBehandling
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.route.BehandlingTilgangsSjekk
import no.nav.etterlatte.libs.ktor.route.PersonTilgangsSjekk
import no.nav.etterlatte.libs.ktor.route.Tilgangssjekker
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import org.slf4j.LoggerFactory
import java.util.UUID

class BehandlingKlientException(
    override val message: String,
    override val cause: Throwable,
) : Exception(message, cause)

class BehandlingKlient(
    config: Config,
    httpClient: HttpClient,
) : BehandlingTilgangsSjekk,
    PersonTilgangsSjekk {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val tilgangssjekker = Tilgangssjekker(config, httpClient)

    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)
    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")

    override suspend fun harTilgangTilBehandling(
        behandlingId: UUID,
        skrivetilgang: Boolean,
        bruker: Saksbehandler,
    ): Boolean = tilgangssjekker.harTilgangTilBehandling(behandlingId, skrivetilgang, bruker)

    override suspend fun harTilgangTilPerson(
        foedselsnummer: Folkeregisteridentifikator,
        skrivetilgang: Boolean,
        bruker: Saksbehandler,
    ): Boolean = tilgangssjekker.harTilgangTilPerson(foedselsnummer, skrivetilgang, bruker)

    suspend fun kanOppdatereTrygdetid(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean {
        logger.info("Sjekker om behandling med behandlingId=$behandlingId kan oppdatere trygdetid")
        val resource = Resource(clientId = clientId, url = "$resourceUrl/behandlinger/$behandlingId/oppdaterTrygdetid")

        return downstreamResourceClient
            .get(resource, brukerTokenInfo)
            .mapBoth(
                success = { true },
                failure = {
                    logger.info("Behandling med id $behandlingId kan ikke oppdatere trygdetid")
                    false
                },
            )
    }

    suspend fun hentSisteIverksatteBehandling(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): SisteIverksatteBehandling {
        logger.info("Henter seneste iverksatte behandling for sak med id $sakId")

        val response =
            downstreamResourceClient.get(
                resource = Resource(clientId = clientId, url = "$resourceUrl/saker/$sakId/behandlinger/sisteIverksatte"),
                brukerTokenInfo = brukerTokenInfo,
            )

        return response.mapBoth(
            success = { deserialize(it.response.toString()) },
            failure = {
                logger.error("Kunne ikke hente seneste iverksatte behandling for sak med id $sakId")
                throw it
            },
        )
    }

    suspend fun settBehandlingStatusTrygdetidOppdatert(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean {
        logger.info("Committer trygdetid oppdatert på behandling med id $behandlingId")
        val response =
            downstreamResourceClient.post(
                resource = Resource(clientId = clientId, url = "$resourceUrl/behandlinger/$behandlingId/oppdaterTrygdetid"),
                brukerTokenInfo = brukerTokenInfo,
                postBody = "{}",
            )

        return response.mapBoth(
            success = { true },
            failure = {
                logger.info("Kunne ikke committe trygdetid oppdatert på behandling med id $behandlingId", it.cause)
                false
            },
        )
    }

    suspend fun hentBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): DetaljertBehandling {
        logger.info("Henter behandling med behandlingId=$behandlingId")

        return retry<DetaljertBehandling> {
            downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/behandlinger/$behandlingId",
                        ),
                    brukerTokenInfo = brukerTokenInfo,
                ).mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        }.let {
            when (it) {
                is RetryResult.Success -> it.content
                is RetryResult.Failure -> {
                    throw BehandlingKlientException(
                        "Klarte ikke hente behandling med behandlingId=$behandlingId",
                        it.samlaExceptions(),
                    )
                }
            }
        }
    }
}
