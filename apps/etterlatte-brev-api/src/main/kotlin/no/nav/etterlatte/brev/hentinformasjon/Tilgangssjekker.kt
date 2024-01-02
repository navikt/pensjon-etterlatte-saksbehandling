package no.nav.etterlatte.brev.hentinformasjon

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.mapBoth
import com.typesafe.config.Config
import io.ktor.client.HttpClient
import no.nav.etterlatte.libs.common.BehandlingTilgangsSjekk
import no.nav.etterlatte.libs.common.PersonTilgangsSjekk
import no.nav.etterlatte.libs.common.SakTilgangsSjekk
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.ktorobo.AzureAdClient
import no.nav.etterlatte.libs.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktorobo.Resource
import no.nav.etterlatte.token.Saksbehandler
import java.util.UUID

class Tilgangssjekker(
    config: Config,
    httpClient: HttpClient,
) : BehandlingTilgangsSjekk, SakTilgangsSjekk, PersonTilgangsSjekk {
    private val azureAdClient = AzureAdClient(config)
    private val downstreamResourceClient = DownstreamResourceClient(azureAdClient, httpClient)

    private val clientId = config.getString("behandling.client.id")
    private val resourceUrl = config.getString("behandling.resource.url")

    override suspend fun harTilgangTilBehandling(
        behandlingId: UUID,
        skrivetilgang: Boolean,
        bruker: Saksbehandler,
    ): Boolean {
        try {
            return downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/tilgang/behandling/$behandlingId?skrivetilgang=$skrivetilgang",
                        ),
                    brukerTokenInfo = bruker,
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
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
                )
                .mapBoth(
                    success = { resource -> resource.response.let { objectMapper.readValue(it.toString()) } },
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
        try {
            return downstreamResourceClient
                .get(
                    resource =
                        Resource(
                            clientId = clientId,
                            url = "$resourceUrl/tilgang/sak/$sakId?skrivetilgang=$skrivetilgang",
                        ),
                    brukerTokenInfo = bruker,
                )
                .mapBoth(
                    success = { deserialize(it.response.toString()) },
                    failure = { throwableErrorMessage -> throw throwableErrorMessage },
                )
        } catch (e: Exception) {
            throw TilgangssjekkException("Sjekking av tilgang for sak feilet", e)
        }
    }
}

class TilgangssjekkException(override val message: String, override val cause: Throwable? = null) :
    Exception(message, cause)
